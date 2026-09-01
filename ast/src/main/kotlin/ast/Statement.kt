package cnc.ast

import cnc.common.Result

// =============================================================================
// Field Types — define qué tipo de dato se extrae de cada segmento
// =============================================================================

enum class FieldType {
    /** Extrae el .text del primer token del segmento */
    TEXT,
    /** Construye una Expression a partir de los tokens del segmento */
    EXPRESSION,
    /** Construye una lista de Expressions */
    EXPRESSIONS
}

// =============================================================================
// Fields — contenedor type-safe de los valores extraídos
// =============================================================================

class Fields(private val data: Map<String, Any>) {

    fun text(name: String): String {
        return data[name] as? String
            ?: error("Field '$name' not found or not a TEXT field")
    }

    fun expression(name: String): Expression {
        return data[name] as? Expression
            ?: error("Field '$name' not found or not an EXPRESSION field")
    }

    @Suppress("UNCHECKED_CAST")
    fun expressions(name: String): List<Expression> {
        return data[name] as? List<Expression>
            ?: error("Field '$name' not found or not an EXPRESSIONS field")
    }

    fun has(name: String): Boolean = data.containsKey(name)

    override fun toString(): String = data.toString()
}

// =============================================================================
// SemanticContext — lo que recibe cada semantic check
// =============================================================================

interface SemanticContext {
    fun isValidType(type: String): Boolean
    fun isDeclared(name: String): Boolean
    fun declare(name: String, type: String)
    fun typeOf(name: String): String?
    fun resolveExpressionType(expr: Expression): Result<String>
}

// =============================================================================
// StatementDef — definición completa de un statement
// =============================================================================

/**
 * Define un tipo de statement de forma declarativa.
 * - tag: identificador del statement
 * - fields: mapa nombre → tipo. Los nombres deben coincidir con los labels de los Steps en la Grammar.
 * - semanticCheck: función de validación semántica
 */
data class StatementDef(
    val tag: String,
    val fields: Map<String, FieldType>,
    val semanticCheck: (Fields, SemanticContext) -> Result<Unit>
)

// =============================================================================
// GenericStatement — instancia concreta producida por el parser
// =============================================================================

data class GenericStatement(
    val def: StatementDef,
    val fields: Fields
) {
    val tag: String get() = def.tag

    override fun toString(): String = "$tag(${fields})"
}
