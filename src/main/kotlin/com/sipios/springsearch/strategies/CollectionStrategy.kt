package com.sipios.springsearch.strategies

import com.sipios.springsearch.SearchOperation
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class CollectionStrategy : ParsingStrategy {
    override fun buildPredicate(
        builder: CriteriaBuilder,
        path: Path<*>,
        fieldName: String,
        ops: SearchOperation?,
        value: Any?
    ): Predicate? {
        if (ops == SearchOperation.IS && value == SearchOperation.EMPTY) {
            return builder.isEmpty(path[fieldName])
        }
        if (ops == SearchOperation.IS_NOT && value == SearchOperation.EMPTY) {
            return builder.isNotEmpty(path[fieldName])
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Unsupported operation $ops $value for collection field $fieldName, " +
                "only IS EMPTY and IS NOT EMPTY are supported"
        )
    }
}
