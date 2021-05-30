import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import java.net.URI;

public class SONMR{

    public static class firstMapper
            extends Mapper<Object, Text, Text, IntWritable>{
        //instance variables
        private int transactions_per_block;
        private int min_supp;
        private int dataset_size;
        private double corr_factor;

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            Configuration conf = context.getConfiguration();
            min_supp = conf.getInt("min_supp",0);
            dataset_size = conf.getInt("dataset_size",0);
            transactions_per_block = conf.getInt("transactions_per_block",0);
            corr_factor = conf.getDouble("corr_factor", 0.0);

            String[] transactions = value.toString().split("\n");
            HashMap<HashSet<Integer> , Integer> itemsets = new HashMap<HashSet<Integer>, Integer>();
            HashMap<HashSet<Integer> , Integer> freqitemsets = new HashMap<HashSet<Integer>, Integer>();
            HashMap<HashSet<Integer> , Integer> freqitemsetsk = new HashMap<HashSet<Integer>, Integer>();

            for (int i =0;i<transactions.length;i++){
                String line = transactions[i];
                String[] items = line.split("\\s+");
                for(int j =0; j<items.length;j++){
                    HashSet<Integer> itemset = new HashSet<Integer>();
                    Integer item = Integer.valueOf(items[j]);
                    itemset.add(item);

                    if (!itemsets.containsKey(itemset)){
                        itemsets.put(itemset, 1);
                        freqitemsetsk.put(itemset,1);
                    }

                    else{
                        itemsets.replace(itemset, itemsets.get(itemset)+1);
                        freqitemsetsk.replace(itemset, itemsets.get(itemset)+1);
                    }
                }
            }
            for ( HashSet<Integer> n : itemsets.keySet()) {
                double thresh = corr_factor * ((double) min_supp / (double) dataset_size ) * (double) transactions_per_block;

                if(itemsets.get(n) < thresh){
                    freqitemsetsk.remove(n);
                }
            }

            for (HashSet<Integer> newFreq:freqitemsetsk.keySet())
            {
                freqitemsets.put(newFreq, 0);
            }


            boolean terminate = false;

            //Apriori
            while(!terminate){

                HashMap<HashSet<Integer> , Integer> freqitemsetsknext = new HashMap<HashSet<Integer>, Integer>();
                HashSet<HashSet<Integer>> nonFreqs = new HashSet<HashSet<Integer>>();

                // create new frequent itemsets
                for (HashSet<Integer> m : freqitemsetsk.keySet()) {
                    for (HashSet<Integer> l : freqitemsetsk.keySet()) {
                        int r = l.size();

                        // create copy to use in making a candidate
                        HashSet<Integer> mCandidate = new HashSet<Integer>();
                        Iterator<Integer> itr_m = m.iterator();
                        while(itr_m.hasNext()){
                            Integer t = itr_m.next();
                            mCandidate.add(t);
                        }
                        mCandidate.addAll(l);

                        if(mCandidate.size()== r+1 && // check size required
                                !freqitemsetsknext.containsKey(mCandidate) && // check if frequent
                                !nonFreqs.contains(mCandidate)){ // check if generated already

                            boolean qualified = true;
                            Iterator<Integer> forM = mCandidate.iterator();
                            HashSet<Integer> mCandidate1 = new HashSet<Integer>();
                            HashSet<Integer> mCandidate2 = new HashSet<Integer>();
                            while(forM.hasNext()) {
                                Integer o = forM.next();
                                mCandidate1.add(o);
                                mCandidate2.add(o);
                            }
                            Iterator<Integer> forM1 = mCandidate1.iterator();
                            while(forM1.hasNext()) {
                                Integer h = forM1.next();
                                mCandidate2.remove(h);

                                if(!freqitemsetsk.containsKey(mCandidate2)){
                                    qualified = false;
                                    break;
                                }
                                mCandidate2.add(h);
                            }
                            if(qualified){
                                freqitemsetsknext.put(mCandidate, 1);
                            }
                            nonFreqs.add(mCandidate);

                        }
                    }

                }

                // calculating the support of each frequent itemset
                for (int k =0;k<transactions.length;k++)
                {
                    String l = transactions[k];
                    for (HashSet<Integer> kp :freqitemsetsknext.keySet())
                    {
                        Iterator<Integer> kp_iter = kp.iterator();
                        boolean find = true;
                        while (kp_iter.hasNext())
                        {
                            Integer anitm = kp_iter.next();
                            String anitem = Integer.toString(anitm.intValue());
                            if (!l.contains(anitem))
                            {
                                find = false;
                                break;
                            }
                        }
                        if (find)
                        {
                            Integer support = freqitemsetsknext.get(kp);
                            freqitemsetsknext.replace(kp, support+1);
                        }
                    }
                }

                // filter out
                double thresh = corr_factor * ((double) min_supp / (double) dataset_size ) * (double) transactions_per_block;
                freqitemsetsknext.entrySet().removeIf(hashSetIntegerEntry -> hashSetIntegerEntry.getValue() < thresh);

                if(freqitemsetsknext.size() != 0){  // if we have new frequent itemsets
                    for (HashSet<Integer> newFreq:freqitemsetsknext.keySet())
                    {
                        freqitemsets.put(newFreq, 0);
                    }
                    freqitemsetsk.clear();
                    freqitemsetsk.putAll(freqitemsetsknext);
                }
                // if no new frequent itemsets are found
                else{
                    terminate = true;
                }
            }

            // The output
            for (HashSet<Integer> kl : freqitemsets.keySet())
            {
                String candidatetoprint = "";
                Iterator<Integer> iter = kl.iterator();
                while (iter.hasNext())
                {
                    Integer i = iter.next();
                    candidatetoprint = candidatetoprint + Integer.toString(i.intValue()) + " ";
                }
                context.write(new Text(candidatetoprint.trim()), new IntWritable(1));
            }

        }
    }

    public static class firstReducer
            extends Reducer<Text,Text,Text,NullWritable>{

        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            context.write(key, NullWritable.get());
        }
    }


    public static class secondMapper extends Mapper<Object, Text, Text, IntWritable> {
        private final IntWritable sum = new IntWritable();
        private final Text word = new Text();
        private HashMap<String, Integer> itemsets = new HashMap<>();

        public void setup(Context context) {

            String path;
            try {
                URI[] myCacheFiles = context.getCacheFiles();
                path = context.getCacheFiles()[0].toString();
                Path forPatterns = new Path(myCacheFiles[0].getPath());
                String patternsFileName = forPatterns.getName();
                File file = new File(patternsFileName);
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(new FileReader(path));

                for (String line; (line = br.readLine()) != null;) {
                    itemsets.put(line, 0);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            String input = value.toString();

            String transactions[] = input.split("\\r?\\n");
            List<String> items = new ArrayList<String>(itemsets.keySet());

            // calculate support of elements
            for (String transaction : transactions) {
                List<String> transItems = Arrays.asList(transaction.split(" "));
                for (String itemset : items) {
                    List<String> elements = Arrays.asList(itemset.split(" "));
                    if (transItems.containsAll(elements)) {
                        itemsets.put(itemset, itemsets.get(itemset) + 1);
                    }
                }
            }

            // write and send values to reducer
            itemsets.entrySet().forEach(entry -> {
                word.set(entry.getKey());
                sum.set(entry.getValue());

                try {
                    context.write(word, sum);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static class secondReducer
            extends Reducer<Text,IntWritable,Text,IntWritable>{
        private int min_supp;

        public void reduce(Text key, Iterable<IntWritable> values,Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            min_supp = conf.getInt("min_supp",0);
            int sum = 0;
            for (IntWritable count:values){
                sum+=count.get();
            }
            if (sum >= min_supp){
                context.write(key, new IntWritable(sum));
            }
        }
    }

    public static void main(String[] args) throws Exception {

        // First Job

        int dataset_size = Integer.parseInt(args[0]);
        int transactions_per_block = Integer.parseInt(args[1]);
        int min_supp = Integer.parseInt(args[2]);
        double corr_factor = Double.parseDouble(args[3]);
        Configuration conf1 = new Configuration();
        conf1.setInt("dataset_size", dataset_size);
        conf1.setInt("transactions_per_block", transactions_per_block);
        conf1.setInt("min_supp", min_supp);
        conf1.setDouble("corr_factor", corr_factor);

        long startJob1 = System.currentTimeMillis();
        Job job1 = Job.getInstance(conf1, "Apriori");
        job1.setInputFormatClass(MultiLineInputFormat.class);
        NLineInputFormat.setNumLinesPerSplit(job1, transactions_per_block);
        job1.setJarByClass(SONMR.class);
        job1.setMapperClass(firstMapper.class);
        job1.setReducerClass(firstReducer.class);
        job1.setOutputKeyClass(Text.class);
        job1.setMapOutputValueClass(IntWritable.class);
        job1.setOutputValueClass(NullWritable.class);
        FileInputFormat.addInputPath(job1, new Path(args[4]));
        FileOutputFormat.setOutputPath(job1, new Path(args[5]));
        job1.waitForCompletion(true);
        long startJob2 = System.currentTimeMillis();

        System.out.println("Job 1 Time: " + (startJob2-startJob1));


        // Second Job

        Configuration conf2 = new Configuration();
        conf2.setInt("dataset_size", dataset_size);
        conf2.setInt("transactions_per_block", transactions_per_block);
        conf2.setInt("min_supp", min_supp);
        conf2.setDouble("corr_factor", corr_factor);
        Job job2 = Job.getInstance(conf2, "phase 2");
        NLineInputFormat.setNumLinesPerSplit(job2, transactions_per_block);
        job2.setInputFormatClass(MultiLineInputFormat.class);
        job2.setJarByClass(SONMR.class);
        job2.setMapperClass(secondMapper.class);
        job2.setReducerClass(secondReducer.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job2, new Path(args[4]));
        Path interm_path = new Path("interm/part-r-00000");
        job2.addCacheFile(interm_path.toUri());
        FileOutputFormat.setOutputPath(job2, new Path(args[6]));
        long endJob2 = System.currentTimeMillis();
        System.out.println("Job 2 Time: " + (endJob2-startJob2));
        System.exit(job2.waitForCompletion(true) ? 0 : 1);

    }

}
