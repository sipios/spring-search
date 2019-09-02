package com.sipios.springsearch

import org.springframework.data.jpa.domain.Specification


class QueryVisitorImpl<T>: QueryBaseVisitor<Specification<T>>() {
    private val ValueRegExp = Regex(pattern = "^(\\*?)(.+?)(\\*?)$")
    override fun visitOpQuery(ctx: QueryParser.OpQueryContext?): Specification<T> {
        val left = visit(ctx?.left)
        val right = visit(ctx?.right)
        val op = ctx?.logicalOp?.text

        when(op) {
            "AND" -> return left.and(right)
            "OR" -> return left.or(right)
            else -> return left.and(right)
        }
    }

    override fun visitPriorityQuery(ctx: QueryParser.PriorityQueryContext?): Specification<T> {
        return visit(ctx?.query())
    }

    override fun visitAtomQuery(ctx: QueryParser.AtomQueryContext?): Specification<T> {
        return visit(ctx?.criteria())
    }

    override fun visitInput(ctx: QueryParser.InputContext?): Specification<T> {
        return visit(ctx?.query())
    }

    override fun visitCriteria(ctx: QueryParser.CriteriaContext?): Specification<T> {
        val key = ctx!!.key()!!.text
        val op = ctx.op()!!.text
        var value = ctx.value()!!.text
        if(ctx.value().STRING() != null) {
            value = value
                    .removeSurrounding("'")
                    .removeSurrounding("\"")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
        }
        val matchResult = this.ValueRegExp.find(value!!)
        val criteria = SearchCriteria(key, op, matchResult!!.groups[1]!!.value, matchResult.groups[2]!!.value, matchResult.groups[3]!!.value)
        return SpecificationImpl(criteria);
    }
}
