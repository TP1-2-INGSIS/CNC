package cnc.semantic.flow

import cnc.common.ErrorType
import cnc.common.Failure
import cnc.common.Result
import cnc.common.Success

/**
 * Monadic, railroad-oriented flow for sequentially performing semantic validations.
 * Transitions to a [FailedSemanticFlow] upon the first failure, rendering subsequent operations
 * no-ops and cleanly returning a [Result] via [.thenOk], [.then], or [.thenResult].
 */
sealed interface SemanticFlow {
    fun expect(condition: Boolean, errorType: ErrorType = ErrorType.SEMANTIC, error: () -> String): SemanticFlow
    fun <T> step(operation: () -> Result<T>, action: (T) -> Unit = {}): SemanticFlow
    fun <T, R> stepAll(items: Iterable<T>, operation: (T) -> Result<R>, action: (List<R>) -> Unit = {}): SemanticFlow
    fun <T, R> optionalStep(item: T?, operation: (T) -> Result<R>, action: (R) -> Unit): SemanticFlow
    fun <T> optionalStep(item: T?, operation: (T) -> Result<Unit>): SemanticFlow

    fun thenOk(): Result<Unit>
    fun <T> then(transform: () -> T): Result<T>
    fun <T> thenResult(operation: () -> Result<T>): Result<T>

    companion object {
        fun ok(): SemanticFlow = OkSemanticFlow
        fun failed(failure: Failure<Nothing>): SemanticFlow = FailedSemanticFlow(failure)
        fun failed(msg: String, errorType: ErrorType = ErrorType.SEMANTIC): SemanticFlow =
            FailedSemanticFlow(Failure(msg, errorType))
    }
}

data object OkSemanticFlow : SemanticFlow {
    override fun expect(condition: Boolean, errorType: ErrorType, error: () -> String): SemanticFlow {
        if (!condition) {
            return FailedSemanticFlow(Failure(error(), errorType))
        }
        return this
    }

    override fun <T> step(operation: () -> Result<T>, action: (T) -> Unit): SemanticFlow {
        return when (val res = operation()) {
            is Success -> {
                action(res.data)
                this
            }
            is Failure -> FailedSemanticFlow(Failure(res.msg, res.type))
        }
    }

    override fun <T, R> stepAll(items: Iterable<T>, operation: (T) -> Result<R>, action: (List<R>) -> Unit): SemanticFlow {
        val results = mutableListOf<R>()
        for (item in items) {
            when (val res = operation(item)) {
                is Success -> results.add(res.data)
                is Failure -> return FailedSemanticFlow(Failure(res.msg, res.type))
            }
        }
        action(results)
        return this
    }

    override fun <T, R> optionalStep(item: T?, operation: (T) -> Result<R>, action: (R) -> Unit): SemanticFlow {
        if (item == null) return this
        return when (val res = operation(item)) {
            is Success -> {
                action(res.data)
                this
            }
            is Failure -> FailedSemanticFlow(Failure(res.msg, res.type))
        }
    }

    override fun <T> optionalStep(item: T?, operation: (T) -> Result<Unit>): SemanticFlow =
        optionalStep(item, operation) {}

    override fun thenOk(): Result<Unit> = Success("ok", Unit)

    override fun <T> then(transform: () -> T): Result<T> = Success("ok", transform())

    override fun <T> thenResult(operation: () -> Result<T>): Result<T> = operation()
}

data class FailedSemanticFlow(val failure: Failure<Nothing>) : SemanticFlow {
    override fun expect(condition: Boolean, errorType: ErrorType, error: () -> String): SemanticFlow = this
    override fun <T> step(operation: () -> Result<T>, action: (T) -> Unit): SemanticFlow = this
    override fun <T, R> stepAll(items: Iterable<T>, operation: (T) -> Result<R>, action: (List<R>) -> Unit): SemanticFlow = this
    override fun <T, R> optionalStep(item: T?, operation: (T) -> Result<R>, action: (R) -> Unit): SemanticFlow = this
    override fun <T> optionalStep(item: T?, operation: (T) -> Result<Unit>): SemanticFlow = this

    override fun thenOk(): Result<Unit> = failure
    override fun <T> then(transform: () -> T): Result<T> = failure
    override fun <T> thenResult(operation: () -> Result<T>): Result<T> = failure
}
