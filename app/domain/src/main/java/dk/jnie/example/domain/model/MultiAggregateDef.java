package dk.jnie.example.domain.model;

import dk.jnie.example.domain.util.ObjectStyle;
import org.immutables.value.Value;

@ObjectStyle
@Value.Immutable
public interface MultiAggregateDef {
    String getAnswer();
}