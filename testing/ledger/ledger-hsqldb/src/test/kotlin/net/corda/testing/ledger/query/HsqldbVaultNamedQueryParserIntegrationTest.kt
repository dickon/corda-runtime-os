package net.corda.testing.ledger.query

import net.corda.db.hsqldb.json.HsqldbJsonExtension.JSON_SQL_TYPE
import net.corda.orm.DatabaseType
import net.corda.orm.DatabaseTypeProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("MaxLineLength")
class HsqldbVaultNamedQueryParserIntegrationTest {

    private val databaseTypeProvider = object : DatabaseTypeProvider {
        override val databaseType: DatabaseType
            get() = DatabaseType.HSQLDB
    }
    private val vaultNamedQueryParser = HsqldbVaultNamedQueryParserImpl(databaseTypeProvider)

    private companion object {
        private fun cast(name: String) = "CAST($name AS $JSON_SQL_TYPE)"

        @JvmStatic
        fun inputsToOutputs(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("WHERE non_json_column = 'some_value'", "WHERE non_json_column = 'some_value'"),
                Arguments.of("WHERE field ->> property = 'some_value'", "WHERE JsonFieldAsText( ${cast("field")}, 'property') = 'some_value'"),
                Arguments.of("WHERE field ->> property = :value", "WHERE JsonFieldAsText( ${cast("field")}, 'property') = :value"),
                Arguments.of(
                    "WHERE \"field name\" ->> \"json property\" = 'some_value'",
                    "WHERE JsonFieldAsText(\"field name\", \"json property\") = 'some_value'"
                ),
                Arguments.of(
                    "WHERE \"field name\" -> \"json property\" ->> \"nested\" = 'some_value'",
                    "WHERE JsonFieldAsText( JsonFieldAsObject(\"field name\", \"json property\"), \"nested\") = 'some_value'"
                ),
                Arguments.of(
                    "WHERE \"field name\" -> \"json property\" -> \"nested\" ->> \"nested_more\" = 'some_value'",
                    "WHERE JsonFieldAsText( JsonFieldAsObject( JsonFieldAsObject(\"field name\", \"json property\"), \"nested\"), \"nested_more\") = 'some_value'"
                ),
                Arguments.of("WHERE (field ->> property)::int = 5", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) = 5"),
                Arguments.of("WHERE (field ->> property)::int != 5", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) != 5"),
                Arguments.of("WHERE (field ->> property)::int < 5", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) < 5"),
                Arguments.of("WHERE (field ->> property)::int <= 5", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) <= 5"),
                Arguments.of("WHERE (field ->> property)::int > 5", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) > 5"),
                Arguments.of("WHERE (field ->> property)::int >= 5", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) >= 5"),
                Arguments.of("WHERE (field ->> property)::int <= :value", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) <= :value"),
                Arguments.of("WHERE (field ->> property)::int = 1234.5678900", "WHERE CAST(( JsonFieldAsText( ${cast("field")}, 'property')) AS int) = 1234.5678900"),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' AND field ->> property2 = 'another value?'",
                    "WHERE JsonFieldAsText( ${cast("field")}, 'property') = 'some_value' AND JsonFieldAsText( ${cast("field")}, 'property2') = 'another value?'"
                ),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' OR field ->> property2 = 'another value'",
                    "WHERE JsonFieldAsText( ${cast("field")}, 'property') = 'some_value' OR JsonFieldAsText( ${cast("field")}, 'property2') = 'another value'"
                ),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' AND field ->> property2 = 'another value' OR field ->> property3 = 'third property?'",
                    "WHERE JsonFieldAsText( ${cast("field")}, 'property') = 'some_value' AND JsonFieldAsText( ${cast("field")}, 'property2') = 'another value' OR JsonFieldAsText( ${cast("field")}, 'property3') = 'third property?'"
                ),
                Arguments.of(
                    "WHERE (field ->> property = 'some_value' AND field ->> property2 = 'another value') OR field ->> property3 = 'third property?'",
                    "WHERE ( JsonFieldAsText( ${cast("field")}, 'property') = 'some_value' AND JsonFieldAsText( ${cast("field")}, 'property2') = 'another value') OR JsonFieldAsText( ${cast("field")}, 'property3') = 'third property?'"
                ),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' AND (field ->> property2 = 'another value') OR field ->> property3 = 'third property')",
                    "WHERE JsonFieldAsText( ${cast("field")}, 'property') = 'some_value' AND ( JsonFieldAsText( ${cast("field")}, 'property2') = 'another value') OR JsonFieldAsText( ${cast("field")}, 'property3') = 'third property')"
                ),
                Arguments.of(
                    "WHERE (field ->> property = 'some_value' AND (field ->> property2 = 'another value') OR field ->> property3 = 'third property'))",
                    "WHERE ( JsonFieldAsText( ${cast("field")}, 'property') = 'some_value' AND ( JsonFieldAsText( ${cast("field")}, 'property2') = 'another value') OR JsonFieldAsText( ${cast("field")}, 'property3') = 'third property'))"
                ),
                Arguments.of("WHERE field ->> property IS NULL", "WHERE JsonFieldAsText( ${cast("field")}, 'property') IS NULL"),
                Arguments.of("WHERE field ->> property IS NOT NULL", "WHERE JsonFieldAsText( ${cast("field")}, 'property') IS NOT NULL"),
                Arguments.of(
                    "WHERE field ->> property IN ('asd', 'fields value', 'asd')",
                    "WHERE JsonFieldAsText( ${cast("field")}, 'property') IN ('asd', 'fields value', 'asd')"
                ),
                Arguments.of(
                    "WHERE (field ->> property IN ('asd', 'fields value', 'asd') AND field ->> property2 = 'another value')",
                    "WHERE ( JsonFieldAsText( ${cast("field")}, 'property') IN ('asd', 'fields value', 'asd') AND JsonFieldAsText( ${cast("field")}, 'property2') = 'another value')"
                ),
                Arguments.of(
                    "WHERE (field ->> property LIKE '%hello there%')",
                    "WHERE ( JsonFieldAsText( ${cast("field")}, 'property') LIKE '%hello there%')"
                ),
                Arguments.of("WHERE field ? property", "WHERE HasJsonKey( ${cast("field")}, 'property')"),
                Arguments.of(
                    """
                        where
                            ("custom"->>'salary'='10'
                            and (custom ->> 'salary')::int>9.00000000
                            or custom ->> 'field with space' is null)
                    """,
                    "WHERE ( JsonFieldAsText( ${cast("\"custom\"")}, 'salary') = '10' AND CAST(( JsonFieldAsText( ${cast("custom")}, 'salary')) AS int) > 9.00000000 OR JsonFieldAsText( ${cast("custom")}, 'field with space') IS NULL)"
                ),
                Arguments.of(
                    """WHERE custom -> 'TestUtxoState' ->> 'testField' = :testField
                        |AND custom -> 'Corda' ->> 'participants' IN :participants
                        |AND custom?:contractStateType
                        |AND created > :created""".trimMargin(),
                    "WHERE JsonFieldAsText( JsonFieldAsObject( ${cast("custom")}, 'TestUtxoState'), 'testField') = :testField AND JsonFieldAsText( JsonFieldAsObject( ${cast("custom")}, 'Corda'), 'participants') IN :participants AND HasJsonKey( ${cast("custom")}, :contractStateType) AND created > :created"
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("inputsToOutputs")
    fun `queries are parsed from a hsqldb query and output back into a hsqldb query`(input: String, output: String) {
        assertThat(vaultNamedQueryParser.parseWhereJson(input)).isEqualTo(output)
    }

    @Test
    fun `queries containing a select throws an exception`() {
        assertThatThrownBy { vaultNamedQueryParser.parseWhereJson("SELECT field") }.isExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `queries containing a from throws an exception`() {
        assertThatThrownBy { vaultNamedQueryParser.parseWhereJson("FROM table") }.isExactlyInstanceOf(IllegalArgumentException::class.java)
    }
}
