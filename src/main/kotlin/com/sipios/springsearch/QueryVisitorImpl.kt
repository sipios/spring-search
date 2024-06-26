package com.sipios.springsearch

import com.sipios.springsearch.anotation.SearchSpec
import com.sipios.springsearch.grammar.QueryBaseVisitor
import com.sipios.springsearch.grammar.QueryParser
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class QueryVisitorImpl<T>(private val searchSpecAnnotation: SearchSpec) : QueryBaseVisitor<Specification<T>>() {
    private val valueRegExp = Regex(pattern = "^(?<prefix>\\*?)(?<value>.+?)(?<suffix>\\*?)$")
    override fun visitOpQuery(ctx: QueryParser.OpQueryContext): Specification<T> {
        val left = visit(ctx.left)
        val right = visit(ctx.right)

        return when (ctx.logicalOp.text) {
            "AND" -> left.and(right)
            "OR" -> left.or(right)
            else -> left.and(right)
        }
    }

    override fun visitPriorityQuery(ctx: QueryParser.PriorityQueryContext): Specification<T> {
        return visit(ctx.query())
    }

    override fun visitAtomQuery(ctx: QueryParser.AtomQueryContext): Specification<T> {
        return visit(ctx.criteria())
    }

    override fun visitInput(ctx: QueryParser.InputContext): Specification<T> {
        return visit(ctx.query())
    }

    override fun visitIsCriteria(ctx: QueryParser.IsCriteriaContext): Specification<T> {
        val key = ctx.key().text
        verifyBlackList(key)
        val op = if (ctx.IS() != null) {
            SearchOperation.IS
        } else {
            SearchOperation.IS_NOT
        }
        return toSpec(key, op, ctx.is_value().text)
    }

    override fun visitEqArrayCriteria(ctx: QueryParser.EqArrayCriteriaContext): Specification<T> {
        val key = ctx.key().text
        verifyBlackList(key)
        val op = if (ctx.IN() != null) {
            SearchOperation.IN_ARRAY
        } else {
            SearchOperation.NOT_IN_ARRAY
        }
        val arr = ctx.array()
        val arrayValues = arr.value()
        val valueAsList: List<String> =
            arrayValues.map { if (it.STRING() != null) clearString(it.text) else it.text }
        // there is no need for prefix and suffix (e.g. 'john*') in case of array value
        val criteria = SearchCriteria(key, op, valueAsList)
        return SpecificationImpl(criteria, searchSpecAnnotation)
    }

    override fun visitBetweenCriteria(ctx: QueryParser.BetweenCriteriaContext): Specification<T> {
        val key = ctx.key().text
        verifyBlackList(key)
        val leftValue = if (ctx.left.STRING() != null) {
            clearString(ctx.left.text)
        } else {
            ctx.left.text
        }
        val rightValue = if (ctx.right.STRING() != null) {
            clearString(ctx.right.text)
        } else {
            ctx.right.text
        }
        val leftExp = toSpec(key, SearchOperation.GREATER_THAN_EQUALS, leftValue)
        val rightExp = toSpec(key, SearchOperation.LESS_THAN_EQUALS, rightValue)
        return if (ctx.BETWEEN() != null) {
            leftExp.and(rightExp)
        } else {
            Specification.not(leftExp.and(rightExp))
        }
    }

    private fun toSpec(
        key: String,
        opLeft: SearchOperation,
        leftValue: String?
    ): SpecificationImpl<T> {
        val criteria = SearchCriteria(key, opLeft, leftValue)
        return SpecificationImpl(criteria, searchSpecAnnotation)
    }

    override fun visitOpCriteria(ctx: QueryParser.OpCriteriaContext): Specification<T> {
        val key = ctx.key().text
        val value = if (ctx.value().STRING() != null) {
            clearString(ctx.value().text)
        } else {
            ctx.value().text
        }
        verifyBlackList(key)
        val matchResult = this.valueRegExp.find(value)
        val op = SearchOperation.getSimpleOperation(ctx.op().text) ?: throw IllegalArgumentException("Invalid operation")
        val criteria = SearchCriteria(
            key,
            op,
            matchResult!!.groups["prefix"]!!.value,
            matchResult.groups["value"]!!.value,
            matchResult.groups["suffix"]!!.value
        )

        return SpecificationImpl(criteria, searchSpecAnnotation)
    }

    private fun verifyBlackList(key: String?) {
        val blackList = this.searchSpecAnnotation.blackListedFields
        if (blackList.contains(key)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Field $key is blacklisted"
            )
        }
    }

    private fun clearString(value: String) = value
        .removeSurrounding("'")
        .removeSurrounding("\"")
        .replace("\\\"", "\"")
        .replace("\\'", "'")
}
