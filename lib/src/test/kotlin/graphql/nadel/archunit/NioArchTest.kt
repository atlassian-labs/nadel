package graphql.nadel.archunit

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaAccess
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

class NioArchTest {
    @Test
    fun `does not use blocking functions`() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("graphql.nadel")

        val rule = noClasses()
            .should()
            .accessTargetWhere(
                object : DescribedPredicate<JavaAccess<*>>("access blocking functions") {
                    override fun test(t: JavaAccess<*>): Boolean {
                        // Java Future
                        if (t.target.owner.isAssignableTo<Future<*>>()) {
                            return t.target.name == "get"
                        }

                        // Coroutines. Use startsWith because sometimes the function is runBlocking$default
                        return t.target.name.startsWith("runBlocking")
                    }
                }
            )

        rule.check(importedClasses)
    }
}
