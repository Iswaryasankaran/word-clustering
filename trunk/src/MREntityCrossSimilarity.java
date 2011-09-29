import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

//import org.apache.hadoop.mapred.TextInputFormat;
//import org.apache.hadoop.mapred.TextOutputFormat;

@SuppressWarnings("deprecation")
public class MREntityCrossSimilarity {

	private static class MyText implements WritableComparable<MyText> {
		private String key;
		private String value;

		MyText(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			// TODO Auto-generated method stub
			int keyLen = in.readInt();
			byte[] keyBytes = new byte[keyLen];
			in.readFully(keyBytes);
			key = new String(keyBytes);

			int valueLen = in.readInt();
			byte[] valueBytes = new byte[valueLen];
			in.readFully(valueBytes);
			value = new String(valueBytes);
		}

		@Override
		public void write(DataOutput out) throws IOException {
			// TODO Auto-generated method stub
			byte[] keyBytes = key.getBytes();
			out.writeInt(keyBytes.length);
			out.write(keyBytes);

			byte[] valueBytes = value.getBytes();
			out.writeInt(valueBytes.length);
			out.write(valueBytes);
		}

		@Override
		public int compareTo(MyText rhs) {
			// TODO Auto-generated method stub
			int compareRes = this.key.compareTo(rhs.key);
			if (compareRes == 0) {
				return this.value.compareTo(rhs.value);
			}
			return compareRes;
		}
		
		public String getValue() {
			return value;
		}
	}

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, Text, MyText> {
		// We should know total records to do the cross similarity.
		int totalRecords = 0;

		@Override
		public void configure(JobConf job) {
			totalRecords = job.getInt("totalRecords", 0);
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, MyText> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("\t");
			Integer recordNumber = Integer.parseInt(parts[0]);
			String newKey = String.format("%010d", recordNumber);
			output.collect(new Text(newKey), new MyText("0", parts[1]));
			System.out.println(newKey + " => " + "0\t" + parts[1]);
			for (int i = 1; i <= totalRecords; ++i) {
				newKey = String.format("%010d", i);
				output.collect(new Text(newKey), new MyText("1", parts[1]));
				System.out.println(newKey + " => " + "1\t" + parts[1]);
			}
		}
	}

	private static class MyPartitioner implements Partitioner<Text, MyText>,
			JobConfigurable {

		@Override
		public int getPartition(Text key, MyText value, int numReduceTasks) {
			Integer partition = Integer.parseInt(key.toString())
					% numReduceTasks;
			return partition;
		}

		@Override
		public void configure(JobConf conf) {
		}
	}

	private static class MyReducer extends MapReduceBase implements
			Reducer<Text, MyText, Text, Text> {

		@Override
		public void reduce(Text key, Iterator<MyText> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			// TODO Auto-generated method stub
			String firstValue = "";
			while (values.hasNext()) {
				String currentValue = values.next().getValue();
				System.out.println(key + " == " + currentValue);
				if (firstValue == "") {
					firstValue = currentValue;
					System.out.println("First value: " + firstValue);
				}
				if (currentValue != firstValue) {
					System.out.println(firstValue + " X " + currentValue);
					output.collect(new Text(firstValue), new Text(firstValue
							+ " X " + currentValue));
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];
		Integer totalRecords = Integer.parseInt(args[2]);

		// int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(MRWordTopicConverter.class);

		conf.setJobName("MRWordTopicConverter");
		conf.setNumReduceTasks(2);
		conf.setInt("totalRecords", totalRecords);

		conf.set("mapred.task.timeout", "12000000");
		// conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		// conf.setInputFormat(TextInputFormat.class);
		// conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setPartitionerClass(MyPartitioner.class);
		conf.setReducerClass(MyReducer.class);

		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(MyText.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
	}
}
