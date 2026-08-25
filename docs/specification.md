# Especificación del Lenguaje PrintScript

Resumen de la sintaxis, tipos de datos y construcciones gramaticales soportadas por el motor de **PrintScript**.

---

## PrintScript 1.0

### Tipos Primitivos
- `string`: Cadenas de texto delimitadas por comillas dobles (`"..."`) o simples (`'...'`).
- `number`: Números de precisión flotante (enteros o decimales).

### Declaración y Asignación de Variables
Las variables se declaran con la palabra clave `let`, son mutables y admiten inicialización opcional:

```typescript
let x: number;
x = 10;

let message: string = "Hola, PrintScript";
message = "Nuevo mensaje";
```

### Operadores Aritméticos
- Suma: `+`
- Resta: `-`
- Multiplicación: `*`
- División: `/`
- Agrupación por paréntesis: `(`, `)`

Ejemplo:
```typescript
let total: number = (5 + 3) * 2 / 4;
```

### Concatenación de Cadenas
El operador `+` permite concatenar cadenas con otras cadenas o números:

```typescript
let result: string = "El total es: " + total;
```

### Salida por Consola
Impresión estándar mediante la función nativa `println`:

```typescript
println("Hola mundo");
println(result);
```

---

## PrintScript 1.1 (Próximamente)

### Tipos Adicionales
- `boolean`: Valores lógicos `true` y `false`.

### Inmutabilidad (`const`)
Declaración de variables cuyo valor no puede ser reasignado:

```typescript
const PI: number = 3.14159;
```

### Estructuras Condicionales (`if` / `else`)
Ejecución condicional basada en expresiones booleanas:

```typescript
if (condicion) {
    println("Verdadero");
} else {
    println("Falso");
}
```

### Funciones de Entrada
- `readInput(prompt: string)`: Lee una línea desde la entrada estándar.
- `readEnv(variableName: string)`: Obtiene el valor de una variable de entorno.
