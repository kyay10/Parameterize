package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public fun parameterize(block: ParameterizeScope.() -> Unit) {
    var iteration: ULong? = 0uL

    while (iteration != null) {
        val context = ParameterizeContext(iteration)
        ParameterizeScope(context).block()

        iteration = context.nextIteration
    }
}

public class ParameterizeScope internal constructor(
    private val context: ParameterizeContext
) {
    public operator fun <T> Parameter<T>.getValue(thisRef: Any?, variable: KProperty<*>): T =
        context.getVariableParameterValue(variable, this)

    public operator fun <T> Parameter.Companion.invoke(
        argumentCount: ULong,
        getArgument: (index: ULong) -> T
    ): Parameter<T> =
        Parameter(context, argumentCount, getArgument)
}

internal class ParameterizeContext(
    private val iteration: ULong
) {
    var nextIteration: ULong? = null
        private set

    private var nextParameterIndex = iteration

    private class VariableParameterValue<out T>(
        val variable: KProperty<*>,
        val parameter: Parameter<T>,
        val value: T
    )

    private val variables = mutableListOf<VariableParameterValue<*>>()

    fun <T> getVariableParameterValue(variable: KProperty<*>, parameter: Parameter<T>): T {
        fun getVariableParameterOrNull(): VariableParameterValue<T>? {
            val variableProperty = variables.firstOrNull { it.variable == variable && it.parameter == parameter }

            @Suppress("UNCHECKED_CAST")
            return variableProperty as VariableParameterValue<T>?
        }

        fun initializeVariableParameter(): VariableParameterValue<T> {
            val parameterIndex = nextParameterIndex % parameter.argumentCount
            nextParameterIndex /= parameter.argumentCount

            if (nextIteration == null) {
                val isLastArgument = parameterIndex == parameter.argumentCount - 1u
                if (!isLastArgument) {
                    nextIteration = iteration + 1u
                }
            }

            val value = parameter.getArgument(parameterIndex)
            val initializedVariable = VariableParameterValue(variable, parameter, value)
            variables += initializedVariable

            return initializedVariable
        }

        val variableProperty = getVariableParameterOrNull() ?: initializeVariableParameter()
        return variableProperty.value
    }
}
