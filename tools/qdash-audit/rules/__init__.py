"""
rules/__init__.py — Rule registry: all rules exported as ALL_RULES list.
"""
from rules.architecture_rules import ARCHITECTURE_RULES
from rules.database_rules     import DATABASE_RULES
from rules.threading_rules    import THREADING_RULES
from rules.rtl_rules          import RTL_RULES
from rules.duplication_rules  import DUPLICATION_RULES
from rules.size_rules         import SIZE_RULES
from rules.orphan_rules       import ORPHAN_RULES

ALL_RULES = (
    ARCHITECTURE_RULES
    + DATABASE_RULES
    + THREADING_RULES
    + RTL_RULES
    + DUPLICATION_RULES
    + SIZE_RULES
    + ORPHAN_RULES
)
