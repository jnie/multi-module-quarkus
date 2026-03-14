package dk.jnie.example.domain.util;

import org.immutables.value.Value;
import org.immutables.value.Value.Style;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom style annotation for Immutables.
 * Applied to interfaces to generate immutable implementations.
 */
@Value.Style(
    typeAbstract = "*Def",
    typeImmutable = "*",
    of = "of",
    allMandatoryParameters = true,
    allParameters = true,
    strictBuilder = true
)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ObjectStyle {
}