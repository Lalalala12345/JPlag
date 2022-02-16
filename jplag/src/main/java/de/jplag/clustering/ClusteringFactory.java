package de.jplag.clustering;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.jplag.JPlagComparison;
import de.jplag.Submission;
import de.jplag.clustering.algorithm.AgglomerativeClustering;
import de.jplag.clustering.algorithm.GenericClusteringAlgorithm;
import de.jplag.clustering.algorithm.SpectralClustering;
import de.jplag.clustering.preprocessors.CdfPreprocessor;
import de.jplag.clustering.preprocessors.PercentileThresholdProcessor;
import de.jplag.clustering.preprocessors.ThresholdPreprocessor;

/**
 * Runs the clustering according to an options object.
 */
public class ClusteringFactory {
    public static List<ClusteringResult<Submission>> getClusterings(Collection<JPlagComparison> comparisons, ClusteringOptions options) {
        if (!options.isEnabled())
            return Collections.emptyList();

        // init algorithm
        GenericClusteringAlgorithm clusteringAlgorithm = null;
        if (options.getAlgorithm() == ClusteringAlgorithm.AGGLOMERATIVE) {
            AgglomerativeClustering.ClusteringOptions clusteringOptions = new AgglomerativeClustering.ClusteringOptions();
            clusteringOptions.minimalSimilarity = options.getAgglomerativeThreshold();
            clusteringOptions.similarity = options.getAgglomerativeInterClusterSimilarity();
            clusteringAlgorithm = new AgglomerativeClustering(clusteringOptions);
        } else if (options.getAlgorithm() == ClusteringAlgorithm.SPECTRAL) {
            SpectralClustering.ClusteringOptions clusteringOptions = new SpectralClustering.ClusteringOptions();
            clusteringOptions.GPVariance = options.getSpectralGaussianProcessVariance();
            clusteringOptions.kernelBandwidth = options.getSpectralKernelBandwidth();
            clusteringOptions.maxKMeansIterations = options.getSpectralMaxKMeansIterationPerRun();
            clusteringOptions.maxRuns = options.getSpectralMaxRuns();
            clusteringOptions.minRuns = options.getSpectralMinRuns();
            clusteringAlgorithm = new SpectralClustering(clusteringOptions);
        }

        // init preprocessor
        ClusteringPreprocessor preprocessor;
        switch (options.getPreprocessor()) {
            case CDF:
                preprocessor = new CdfPreprocessor();
                break;
            case THRESHOLD:
                preprocessor = new ThresholdPreprocessor(options.getPreprocessorThreshold());
                break;
            case PERCENTILE:
                preprocessor = new PercentileThresholdProcessor(options.getPreprocessorPercentile());
                break;
            case NONE:
            default:
                preprocessor = null;
                break;
        }
        if (preprocessor != null) {
            // Package preprocessor into a clustering algorithm
            clusteringAlgorithm = new ClusteringPreprocessor.PreprocessedClusteringAlgorithm(clusteringAlgorithm, preprocessor);
        }

        // init adapter
        ClusteringAdapter adapter = new ClusteringAdapter(comparisons, options.getSimilarityMetric());

        // run clustering
        ClusteringResult<Submission> result = adapter.doClustering(clusteringAlgorithm);

        // remove bad clusters
        result = removeBadClusters(result);

        return List.of(result);
    }

    private static ClusteringResult<Submission> removeBadClusters(final ClusteringResult<Submission> clustering) {
        List<Cluster<Submission>> filtered = clustering.getClusters().stream().filter(cluster -> !cluster.isBadCluster())
                .collect(Collectors.toList());
        return new ClusteringResult<>(filtered, clustering.getCommunityStrength());
    }
}
