package bdc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable; 
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import utilities.SortUtility;

public class TopView {
	//Mapper For Movies
	public static class MovMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String record = value.toString();
			String[] parts = record.split("::");
			context.write(new Text(parts[0]), new Text("mv\t" + parts[1]));
		}
	}
	//Mapper For Ratings
	public static class RatMapper extends
			Mapper<LongWritable, Text, Text, Text> {
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String record = value.toString();
			String[] parts = record.split("::");
			context.write(new Text(parts[1]), new Text("rt\t"));
		}
	}
	//Reduce Join
	public static class ReduceJoinReducer extends
			Reducer<Text, Text, Text, Text> {
		private Map<Text, IntWritable> cMap = new HashMap<>();
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String name = "";
			int count = 0;
			for (Text t : values) {
				String parts[] = t.toString().split("\t");
				if (parts[0].equals("rt")) {
					count++;
				} else if (parts[0].equals("mv")) {
					name = parts[1];
				}
			}
			cMap.put(new Text(key + "\t" +name), new IntWritable(count));
		}
		 //Sorting and cleaning for top 10 data
		protected void cleanup(Context context) throws IOException, InterruptedException {

	            Map<Text, IntWritable> sortedMap = SortUtility.sortByValues(cMap);

	            int counter = 0;
	            for (Text key : sortedMap.keySet()) {
	                if (counter++ == 10) {
	                    break;
	                }
	                context.write(key , new Text(sortedMap.get(key).toString()));
	            }
	        }
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = new Job(conf);
		job.setJarByClass(TopView.class);
		job.setReducerClass(ReduceJoinReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
	
		MultipleInputs.addInputPath(job, new Path(args[0]),TextInputFormat.class, MovMapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]),TextInputFormat.class, RatMapper.class);
		Path outputPath = new Path(args[2]);
		
		job.setOutputFormatClass(TextOutputFormat.class);
                                  FileOutputFormat.setOutputPath(job, new Path(args[2]));
		
		job.waitForCompletion(true) ;
	}
}
