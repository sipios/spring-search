package com.sipios.springsearch

class SearchCriteria // Change EQUALS into ENDS_WITH, CONTAINS, STARTS_WITH based on the presence of * in the value
    (var key: String, private var op: SearchOperation, prefix: String?, val value: Any?, suffix: String?) {
    var operation: SearchOperation?

    init {
        // Change EQUALS into ENDS_WITH, CONTAINS, STARTS_WITH based on the presence of * in the value
        val startsWithAsterisk = prefix != null && prefix.contains(SearchOperation.ZERO_OR_MORE_REGEX)
        val endsWithAsterisk = suffix != null && suffix.contains(SearchOperation.ZERO_OR_MORE_REGEX)
        op = when {
            op === SearchOperation.EQUALS && startsWithAsterisk && endsWithAsterisk -> SearchOperation.CONTAINS
            op === SearchOperation.EQUALS && startsWithAsterisk -> SearchOperation.ENDS_WITH
            op === SearchOperation.EQUALS && endsWithAsterisk -> SearchOperation.STARTS_WITH
            else -> op
        }

        // Change NOT_EQUALS into DOESNT_END_WITH, DOESNT_CONTAIN, DOESNT_START_WITH based on the presence of * in the value
        op = when {
            op === SearchOperation.NOT_EQUALS && startsWithAsterisk && endsWithAsterisk -> SearchOperation.DOESNT_CONTAIN
            op === SearchOperation.NOT_EQUALS && startsWithAsterisk -> SearchOperation.DOESNT_END_WITH
            op === SearchOperation.NOT_EQUALS && endsWithAsterisk -> SearchOperation.DOESNT_START_WITH
            else -> op
        }
        this.operation = op
    }
    constructor(key: String, op: SearchOperation, value: Any?) : this(key, op, null, value, null) {
    }
}
