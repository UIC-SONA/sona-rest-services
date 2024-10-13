package ec.gob.conagopare.sona.utils.stats;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Data
public class StatData<L> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String title;
    private final List<L> labels;
    private final List<Dataset> datasets;

    public StatData(String title) {
        this.title = title;
        this.labels = new ArrayList<>();
        this.datasets = new ArrayList<>();
    }

    public void addLabel(L label) {
        this.labels.add(label);
    }

    public final void addLabels(List<L> labels) {

        this.labels.addAll(labels);
    }

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
    }

    public void addDataset(String label, List<Number> data) {
        addDataset(new Dataset(label, data));
    }

    public void addDatasets(List<? extends OrderedPair<L>> pairs, String labelString) {
        var x = OrderedPair.x(pairs);
        if (labels.isEmpty()) {
            x.forEach(this::addLabel);
            addDataset(new Dataset(labelString, OrderedPair.y(pairs)));
            return;
        }

        final var data = new ArrayList<Number>();
        for (var label : labels) {
            var y = OrderedPair.f(pairs, label);
            data.add(y.orElse(null));
        }

        addDataset(new Dataset(labelString, data));

    }

    public static <L> @NotNull StatData<L> singleDataset(
            List<? extends OrderedPair<L>> pairs,
            List<L> labels,
            String title,
            String labelString
    ) {
        var stat = new StatData<L>(title);
        stat.addLabels(labels);
        var dataset = new Dataset(labelString, OrderedPair.y(pairs));
        stat.addDataset(dataset);
        return stat;
    }

    public static <L> @NotNull StatData<L> singleDataset(
            List<? extends OrderedPair<L>> pairs,
            String title,
            String labelString
    ) {
        return singleDataset(pairs, OrderedPair.x(pairs), title, labelString);
    }

    public static <L, G> @NotNull StatData<L> generate(
            List<? extends OrderedPair<G>> pairs,
            List<L> labels,
            Function<G, List<? extends OrderedPair<L>>> pairsToDatasetExtractor,
            Function<G, String> labelMapper,
            String title
    ) {
        StatData<L> stats = new StatData<>(title);
        stats.addLabels(labels);
        OrderedPair.x(pairs).forEach(x -> {
            List<? extends OrderedPair<L>> orderedPairs = pairsToDatasetExtractor.apply(x);
            stats.addDatasets(orderedPairs, labelMapper.apply(x));
        });
        return stats;
    }


    @Data
    @AllArgsConstructor
    public static class Dataset {
        private String label;
        private List<Number> data;
    }
}