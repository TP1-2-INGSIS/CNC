package cnc.parser.expression

import cnc.ast.BinaryExpression
import cnc.ast.Expression
import cnc.ast.UnaryExpression
import cnc.ast.CallExpression
import cnc.ast.Identifier
import cnc.common.Cursor
import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success
import cnc.common.asCursor
import cnc.token.Token
import cnc.token.TokenDefinition

enum class Associativity { LEFT, RIGHT }

data class OperatorDef(
    val definition: TokenDefinition,
    val precedence: Int,
    val associativity: Associativity = Associativity.LEFT
)

data class PrefixOperatorDef(
    val definition: TokenDefinition,
    val precedence: Int
)

interface ExpressionParser {
    fun parse(cursor: Cursor<Token>): Result<Expression>
}

/**
 * Pratt parser implementation for expressions with precedence and associativity,
 * returning pure [Result]s with zero exceptions for control flow.
 */
class ExpressionBuilder(
    val recipes: Map<TokenDefinition, (Token) -> Expression>,
    val operators: List<OperatorDef> = emptyList(),
    val prefixOperators: List<PrefixOperatorDef> = emptyList(),
    val groupOpen: TokenDefinition? = null,
    val groupClose: TokenDefinition? = null,
    val callOpen: TokenDefinition? = groupOpen,
    val callClose: TokenDefinition? = groupClose,
    val callSeparator: TokenDefinition? = null
) : ExpressionParser {

    fun copy(
        recipes: Map<TokenDefinition, (Token) -> Expression> = this.recipes,
        operators: List<OperatorDef> = this.operators,
        prefixOperators: List<PrefixOperatorDef> = this.prefixOperators,
        groupOpen: TokenDefinition? = this.groupOpen,
        groupClose: TokenDefinition? = this.groupClose,
        callOpen: TokenDefinition? = this.callOpen,
        callClose: TokenDefinition? = this.callClose,
        callSeparator: TokenDefinition? = this.callSeparator
    ): ExpressionBuilder = ExpressionBuilder(
        recipes = recipes,
        operators = operators,
        prefixOperators = prefixOperators,
        groupOpen = groupOpen,
        groupClose = groupClose,
        callOpen = callOpen,
        callClose = callClose,
        callSeparator = callSeparator
    )

    fun build(token: Token): Result<Expression> {
        val (_, builder) = recipes.entries.firstOrNull { (definition, _) ->
            definition.match(token.text)
        } ?: return Failure(
            "No recipe matches token: '${token.text}' at row ${token.pos.row}, col ${token.pos.col}",
            ErrorType.PARSER
        )
        return Success("Parsed token", builder(token))
    }

    fun build(tokens: List<Token>): Result<Expression> {
        if (tokens.isEmpty()) return Failure(
            "Cannot build expression from empty token list",
            ErrorType.PARSER
        )
        if (operators.isEmpty() || tokens.size == 1) {
            return build(tokens.first())
        }
        val cursor = tokens.asCursor()
        val result = parse(cursor)
        if (result is Failure) return result
        if (cursor.hasMore()) {
            return Failure(
                "Unexpected token '${cursor.peek()?.text}' after expression",
                ErrorType.PARSER
            )
        }
        return result
    }

    override fun parse(cursor: Cursor<Token>): Result<Expression> = parseExpression(cursor, 0)

    private fun parseExpression(cursor: Cursor<Token>, minPrecedence: Int): Result<Expression> {
        val leftResult = parseAtom(cursor)
        if (leftResult is Failure) return leftResult
        var left = (leftResult as Success).data

        while (cursor.hasMore()) {
            val token = cursor.peek() ?: break

            // Postfix function call: identifier followed by callOpen
            if (callOpen != null && callOpen.match(token.text) && left is Identifier) {
                val callRes = parseCall(cursor, left)
                if (callRes is Failure) return callRes
                left = (callRes as Success).data
                continue
            }

            val operator = findOperator(token) ?: break
            if (operator.precedence < minPrecedence) break

            cursor.advance()

            val nextMinPrecedence = if (operator.associativity == Associativity.LEFT) {
                operator.precedence + 1
            } else {
                operator.precedence
            }

            val rightRes = parseExpression(cursor, nextMinPrecedence)
            if (rightRes is Failure) return rightRes
            val right = (rightRes as Success).data

            left = BinaryExpression(left, token.text, right)
        }

        return Success("Parsed expression", left)
    }

    private fun parseAtom(cursor: Cursor<Token>): Result<Expression> {
        if (!cursor.hasMore()) {
            return Failure(
                "Unexpected end of expression, expected a value",
                ErrorType.PARSER
            )
        }

        val token = cursor.peek()!!

        val prefixOp = findPrefixOperator(token)
        if (prefixOp != null) {
            cursor.advance()
            val operandRes = parseExpression(cursor, prefixOp.precedence)
            return when (operandRes) {
                is Success -> Success("Parsed unary expression", UnaryExpression(token.text, operandRes.data))
                is Failure -> operandRes
            }
        }

        if (groupOpen != null && groupOpen.match(token.text)) {
            cursor.advance()
            val exprRes = parseExpression(cursor, 0)
            if (exprRes is Failure) return exprRes

            val closing = cursor.peek()
            if (closing == null || groupClose == null || !groupClose.match(closing.text)) {
                val expected = groupClose?.symbols?.firstOrNull() ?: ")"
                return Failure(
                    "Expected closing '$expected' after grouped expression",
                    ErrorType.PARSER
                )
            }
            cursor.advance()
            return exprRes
        }

        cursor.advance()
        val (_, recipe) = recipes.entries.firstOrNull { (def, _) -> def.match(token.text) }
            ?: return Failure(
                "No recipe matches token: '${token.text}' at row ${token.pos.row}, col ${token.pos.col}",
                ErrorType.PARSER
            )

        return Success("Parsed atom", recipe(token))
    }

    private fun parseCall(cursor: Cursor<Token>, target: Identifier): Result<Expression> {
        cursor.advance() // consume callOpen
        val args = mutableListOf<Expression>()

        val firstToken = cursor.peek()
        if (firstToken != null && (callClose == null || !callClose.match(firstToken.text))) {
            val argRes = parseExpression(cursor, 0)
            if (argRes is Failure) return argRes
            args.add((argRes as Success).data)

            while (cursor.peek()?.let { callSeparator?.match(it.text) } == true) {
                cursor.advance() // consume separator (comma)
                val nextArgRes = parseExpression(cursor, 0)
                if (nextArgRes is Failure) return nextArgRes
                args.add((nextArgRes as Success).data)
            }
        }

        val closeToken = cursor.peek()
        if (closeToken == null || callClose == null || !callClose.match(closeToken.text)) {
            val expected = callClose?.symbols?.firstOrNull() ?: ")"
            return Failure("Expected closing '$expected' after arguments", ErrorType.PARSER)
        }
        cursor.advance() // consume callClose

        return Success("Parsed call expression", CallExpression(target.name, args))
    }

    private fun findOperator(token: Token): OperatorDef? =
        operators.firstOrNull { it.definition.match(token.text) }

    private fun findPrefixOperator(token: Token): PrefixOperatorDef? =
        prefixOperators.firstOrNull { it.definition.match(token.text) }
}
