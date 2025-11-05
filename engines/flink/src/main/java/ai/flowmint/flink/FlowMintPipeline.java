package ai.flowmint.flink;

import ai.flowmint.flink.loader.PlanLoader;
import ai.flowmint.flink.model.Plan;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main FlowMint Flink pipeline that reads files, processes records, and emits metrics.
 */
public class FlowMintPipeline {
    
    private static final Logger LOG = LoggerFactory.getLogger(FlowMintPipeline.class);
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: FlowMintPipeline <plan-yaml-path> <input-file-path>");
            System.exit(1);
        }
        
        String planPath = args[0];
        String inputPath = args[1];
        
        LOG.info("Starting FlowMint pipeline with plan: {}, input: {}", planPath, inputPath);
        
        // Load plan
        PlanLoader loader = new PlanLoader();
        Plan plan;
        try {
            plan = loader.loadFromFile(planPath);
            loader.validate(plan);
            LOG.info("Loaded plan: {}", plan.getName());
        } catch (IOException e) {
            LOG.error("Failed to load plan from {}", planPath, e);
            throw e;
        }
        
        // Create Flink environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1); // Start with single parallelism for simplicity
        
        // Create file source
        FileSource<String> fileSource = FileSource
                .forRecordStreamFormat(new TextLineInputFormat(), new Path(inputPath))
                .build();
        
        // Read file lines
        DataStream<String> lines = env.fromSource(
                fileSource,
                WatermarkStrategy.noWatermarks(),
                "FileSource",
                TypeInformation.of(String.class)
        );
        
        // Process records with metrics
        DataStream<String> processed = lines
                .map(new RecordProcessorFunction(plan))
                .name("RecordProcessor");
        
        // Sink to console
        processed.addSink(new PrintSinkFunction<>("Processed: "))
                .name("ConsoleSink");
        
        // Execute pipeline
        env.execute("FlowMint Pipeline: " + plan.getName());
    }
    
    /**
     * Map function that processes records and emits metrics.
     */
    private static class RecordProcessorFunction extends RichMapFunction<String, String> {
        
        private static final Logger LOG = LoggerFactory.getLogger(RecordProcessorFunction.class);
        
        private transient Counter recordCounter;
        private transient Meter recordRate;
        private final Plan plan;
        
        public RecordProcessorFunction(Plan plan) {
            this.plan = plan;
        }
        
        @Override
        public void open(Configuration parameters) {
            recordCounter = getRuntimeContext()
                    .getMetricGroup()
                    .counter("records_processed_total");
            
            recordRate = getRuntimeContext()
                    .getMetricGroup()
                    .meter("records_processed_rate", new org.apache.flink.metrics.MeterView(recordCounter));
            
            LOG.info("RecordProcessor initialized for plan: {}", plan.getName());
        }
        
        @Override
        public String map(String value) throws Exception {
            // Increment counter
            recordCounter.inc();
            
            // Log every 100 records
            long count = recordCounter.getCount();
            if (count % 100 == 0) {
                LOG.info("Processed {} records (rate: {}/s)", count, recordRate.getRate());
            }
            
            // For now, just pass through the line
            // In future sprints, this will parse, validate, and transform
            return value;
        }
    }
}

