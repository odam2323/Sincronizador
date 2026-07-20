package org.example.sincronizador2.services.particion;

import org.example.sincronizador2.enums.MetodoParticion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PartitionStrategyResolver {

    private final Map<MetodoParticion, PartitionStrategy> estrategias;

    public PartitionStrategyResolver(List<PartitionStrategy> lista) {
        this.estrategias = lista.stream()
                .collect(Collectors.toMap(PartitionStrategy::metodo, Function.identity()));
    }

    public PartitionStrategy resolver(MetodoParticion metodo) {
        PartitionStrategy s = estrategias.get(metodo);
        if (s == null) {
            throw new IllegalArgumentException("No hay estrategia para el método: " + metodo);
        }
        return s;
    }
}