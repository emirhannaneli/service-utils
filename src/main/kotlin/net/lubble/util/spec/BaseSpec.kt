package net.lubble.util.spec

import net.lubble.util.model.ParameterModel

/**
 * Abstract base class for specifications.
 *
 * @param T the type of the JPA model
 * @param params the parameters for the specification
 * @param fields the fields to projection
 */
abstract class BaseSpec<T>(params: ParameterModel, val fields: Collection<String>? = null) : SpecTool(params), SpecTool.JPAModel<T>