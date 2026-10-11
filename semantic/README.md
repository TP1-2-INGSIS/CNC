# Módulo: :semantic

**Ruta:** `/semantic`  
**Dependencias directas:** `:common`, `:ast`  
**Consumidores:** `:printscript` (perfiles v1.0 y v1.1), `:app`, `:cli`  

---

## 1. Responsabilidad y Propósito (SRP)
- **Qué problema resuelve:** Realiza la comprobación estática de tipos (*type-checking*), la verificación de alcance de identificadores (*scope checking*), previene la redeclaración de variables, asegura la inmutabilidad de constantes (`const`), valida condiciones booleanas en sentencias de control (`if`/`else`) y verifica compatibilidad en operaciones binarias, unarias y llamadas a funciones.
- **Qué hace:**
  - Valida sentencias desacopladas mediante el patrón de reglas extensibles `StatementValidator<T>` (`Declaration`, `Assignment`, `Call`, `BlockStatement`, `IfStatement`).
  - Mantiene la tabla de símbolos jerárquica (`SymbolTable`) con encadenamiento léxico hacia el padre (`parent`).
  - Resuelve recursivamente el tipo estático de cualquier expresión (`ExpressionTypeResolver`) con cero alocaciones en hot paths.
  - Verifica llamadas a funciones utilizando la abstracción compartida `BuiltinMethod` (proveniente de `:common`).
  - Provee resolvedores de tipos estándar (`TypeResolvers`: suma/concatenación, aritmética estricta, operadores unarios numéricos y lógicos `!`).
  - Emite errores semánticos puramente funcionales (`Result<T>` con `ErrorType.SEMANTIC`).
- **Qué NO hace (Fronteras):**
  - No ejecuta sentencias ni calcula valores numéricos/texto en tiempo de ejecución (responsabilidad de `:interpreter`).
  - No re-analiza sintaxis ni consume tokens directamente (responsabilidad de `:parser`).

---

## 2. Arquitectura y Componentes Clave

### Diagrama de Arquitectura
```mermaid
flowchart TD
    Stmt["Sequence<Statement>"] --> Analyzer["SemanticAnalyzer.analyze()"]
    Analyzer --> ValRegistry["StatementValidator<T> Registry"]
    
    ValRegistry -->|Declaration| DeclVal["DeclarationValidator"]
    ValRegistry -->|Assignment| AssignVal["AssignmentValidator"]
    ValRegistry -->|Call| CallVal["CallValidator (builtins check)"]
    ValRegistry -->|BlockStatement| BlockVal["BlockValidator (child scope)"]
    ValRegistry -->|IfStatement| IfVal["IfValidator (boolean cond)"]
    
    DeclVal --> ST["SymbolTable (declare / typeOf)"]
    AssignVal --> ETR["ExpressionTypeResolver.resolve()"]
    CallVal --> ETR
    IfVal --> ETR
    
    ETR --> ETRules["ExpressionTypeRule (AST Nodes)"]
    ETRules --> TypeRes["TypeResolvers (Binary & Unary rules)"]
    
    Analyzer --> Result["Sequence<Result<Statement>>"]
```

### Entidades de Dominio e Interfaces
1. **`SemanticAnalyzer`:**
   - Orquestador desacoplado: `fun analyze(statements: Sequence<Statement>): Sequence<Result<Statement>>`.
   - Despacha cada sentencia a su `StatementValidator<Statement>` registrado en un mapa de configuración, logrando simetría total con el `:interpreter` y `:parser` y eliminando cualquier bloque cerrado `when`.
2. **`StatementValidator<T : Statement>`:**
   - Interfaz funcional: `fun validate(statement: T, context: SemanticContext, analyzer: SemanticAnalyzer): Result<Unit>`.
   - `StandardStatementValidators.v1_0`: Soporta `Declaration`, `Assignment` y `Call`.
   - `StandardStatementValidators.v1_1`: Extiende con `BlockStatement` e `IfStatement`.
3. **`SemanticContext`:**
   - Interfaz de contexto: `isValidType`, `isDeclaredInCurrentScope`, `declare`, `typeOf`, `isMutable`, `resolveExpressionType`, `createChildScope`, `isFunctionDeclared`, `functionReturnType`.
   - `DefaultSemanticContext`: Conecta `SymbolTable`, `BinaryOpResolver`, `UnaryOpResolver`, `ExpressionTypeRule` y `BuiltinMethod`.
4. **`SymbolTable`:**
   - Almacén de tipos válidos y variables registradas con mutabilidad. Soporta ámbitos anidados mediante `createChild()`, resolviendo variables subiendo por la cadena de padres en $O(\text{profundidad})$ sin alocaciones de memoria.
5. **`ExpressionTypeResolver`:**
   - Infiere recursivamente el tipo estático de expresiones mediante reglas componibles `ExpressionTypeRule<T>`. Consulta tipos de variables delegando directamente a `symbolTable::typeOf` ($O(1)$ allocations) y valida funciones contra `BuiltinMethod`.
6. **`TypeResolvers`:**
   - Reglas de compatibilidad de tipos:
     - `additionOrConcat`: `number + number = number`, `string + anything = string`.
     - `numericOnly(op)`: Operaciones aritméticas estrictas (`-`, `*`, `/`, `**`).
     - `unaryNumeric(op)`: `-` unario exige `number`.
     - `booleanNot`: `!` unario exige `boolean`.

---

## 3. Manejo de Errores y Pipeline Funcional
- **ErrorType asociado:** `ErrorType.SEMANTIC`.
- **Garantías:** Cero excepciones. Errores tales como:
  - Variable ya declarada o no declarada.
  - Tipo no reconocido.
  - Reasignación de constantes (`const`).
  - Condición no booleana en sentencias `if`.
  - Incompatibilidad de tipos en asignaciones y operaciones binarias/unarias.
  - Funciones no declaradas o funciones sin retorno (`println`) utilizadas como expresiones.
  Todos son modelados funcionalmente como `Failure("...", ErrorType.SEMANTIC)`.

---

## 4. Integración con Builtins Compartidos (`BuiltinMethod`)
El tipo `BuiltinMethod` reside en `:common` y contiene tanto la metadata semántica (`name`, `returnType`) como la ejecución para el intérprete. Esto permite que los perfiles de lenguaje (v1.0, v1.1, v1.3) definan su conjunto de builtins **una única vez** y los compartan de manera idéntica entre el `SemanticAnalyzer` y el `Interpreter`.

---

## 5. Extensibilidad (Open/Closed Principle)
Para agregar una nueva sentencia (ej: `WhileStatement`) o nueva expresión (ej: `TernaryExpression`):
```kotlin
// 1. Crear el validador
val whileValidator = StatementValidator<WhileStatement> { stmt, ctx, analyzer ->
    val condRes = ctx.resolveExpressionType(stmt.condition)
    if (condRes is Failure) return@StatementValidator condRes
    analyzer.validate(stmt.body, ctx)
}

// 2. Componer en el analizador sin tocar el código fuente existente
val customAnalyzer = SemanticAnalyzer(
    context = customContext,
    statementValidators = StandardStatementValidators.v1_1 + mapOf(
        WhileStatement::class to whileValidator
    )
)
```
