package com.sipios.springsearch.strategies

import com.sipios.springsearch.SearchOperation
import com.sipios.springsearch.anotation.SearchSpec
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Path
import javax.persistence.criteria.Predicate

class StringStrategy(var searchSpecAnnotation: SearchSpec) : ParsingStrategy {
    override fun buildPredicate(
        builder: CriteriaBuilder,
        path: Path<*>,
        fieldName: String,
        ops: SearchOperation?,
        value: Any?
    ): Predicate? {
        if (!searchSpecAnnotation.caseSensitiveFlag) {
            val lowerCasedValue = (value as String).toLowerCase()
            val loweredFieldValue = builder.lower(path.get(fieldName))
            return when (ops) {
                SearchOperation.STARTS_WITH -> builder.like(loweredFieldValue, "$lowerCasedValue%")
                SearchOperation.ENDS_WITH -> builder.like(loweredFieldValue, "%$lowerCasedValue")
                SearchOperation.CONTAINS -> builder.like((loweredFieldValue), "%$lowerCasedValue%")
                SearchOperation.DOESNT_START_WITH -> builder.notLike(loweredFieldValue, "$lowerCasedValue%")
                SearchOperation.DOESNT_END_WITH -> builder.notLike(loweredFieldValue, "%$lowerCasedValue")
                SearchOperation.DOESNT_CONTAIN -> builder.notLike(loweredFieldValue, "%$lowerCasedValue%")
                else -> super.buildPredicate(builder, path, fieldName, ops, value)
            }
        }
        return super.buildPredicate(builder, path, fieldName, ops, value)
    }
}
