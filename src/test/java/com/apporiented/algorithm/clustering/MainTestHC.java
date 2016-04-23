package test.java.com.apporiented.algorithm.clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import main.java.com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.Cluster;
import main.java.com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

public class MainTestHC {
	private int NUMBER_OF_PROCESSORS = 16;
	
	private String[] modelList = {
			"CCCma-CanESM2_CCCma-CanRCM4",
		    "CCCma-CanESM2_SMHI-RCA4",
		    "CNRM-CERFACS-CNRM-CM5_CLMcom-CCLM4-8-17",
		    "CNRM-CERFACS-CNRM-CM5_SMHI-RCA4",
		    "CSIRO-QCCCE-CSIRO-Mk3-6-0_SMHI-RCA4",
		    "ICHEC-EC-EARTH_CLMcom-CCLM4-8-17",
		    "ICHEC-EC-EARTH_DMI-HIRHAM5",
		    "ICHEC-EC-EARTH_KNMI-RACMO22T",
		    "ICHEC-EC-EARTH_SMHI-RCA4",
		    "IPSL-IPSL-CM5A-MR_SMHI-RCA4",
		    "MIROC-MIROC5_SMHI-RCA4_v1",
		    "MOHC-HadGEM2-ES_CLMcom-CCLM4-8-17",
		    "MOHC-HadGEM2-ES_SMHI-RCA4",
		    "MOHC-HadGEM2-ES_KNMI-RACMO22T_v1",
		    "MPI-M-MPI-ESM-LR_CLMcom-CCLM4-8-17",
		    "MPI-M-MPI-ESM-LR_SMHI-RCA4",
		    "NCC-NorESM1-M_SMHI-RCA4",
		    "NOAA-GFDL-GFDL-ESM2M_SMHI-RCA4"
	};
	
	private String[] metricList = {"TimeMean", "TimeSkewness", "TimeKurtosis", "TimeEntropy", "TimeQuadraticScore", "TimeCV", "TimeStd", "TimeIQR"};
	public String variable = "";
	
//	public static void main(String[] args){
//		MainTestHC self = new MainTestHC();
//		self.variable = args[0];
//		int groupid = Integer.valueOf(args[1]);
//		HashMap<String, TiffParser> data = new HashMap<String, TiffParser>();
//	    HashMap<String, File> filePath = new HashMap<String, File>();
//	    for(int i=0; i<self.modelList.length; i++){
//	    	String baseDir = "/work/asu/data/CalculationResults/" + self.variable + "/" + self.metricList[groupid] + "/";
//	    	String key = self.modelList[i];
//	    	File file = self.getAllFiles(baseDir, key);
//    		filePath.put(key, file);
//	    }
//	    data = self.loadTiffFiles(filePath, self.modelList);
//	    HashMap<String, float[]> results = new HashMap<String, float[]>();
//	    double width = data.get(self.modelList[0]).getSize()[1];
//		double height = data.get(self.modelList[0]).getSize()[0];
//		int k=0;
//	    for(int i=0; i<self.modelList.length-1; i++){
//	    	for(int j=i+1; j<self.modelList.length; j++){
//	    		float[] tempData = new float[(int) (width*height)];
//	    		Arrays.fill(tempData, Float.NaN);
//		    	results.put(String.valueOf(k), tempData);
//		    	k++;
//	    	}
//	    }
//	    float[] efforResult = new float[(int) (width*height)];
//	    Arrays.fill(efforResult, Float.NaN);
//	    self.hierarchicalclusterEntry(data, results, efforResult);
//	    k=0;
//	    for(int i=0; i<self.modelList.length-1; i++){
//	    	for(int j=i+1; j<self.modelList.length; j++){
//	    		String outputfile = "/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/HierarchicalClst/"+ self.metricList[groupid] + "/" + self.modelList[i] + "_" + self.modelList[j] + ".tif";
//		    	self.saveTiff(data.get(self.modelList[0]), outputfile, results.get(String.valueOf(k)));
//	    		k++;
//	    	}
//	    }
//	    String outputfile = "/work/asu/data/CalculationResults/pr_HIST/SimilarityResults/HierarchicalClst/" + self.metricList[groupid]  + "/" +  "effort.tif";
//    	self.saveTiff(data.get(self.modelList[0]), outputfile, efforResult);
//	}
	

	
	public void hierarchicalclusterEntry(HashMap<String, TiffParser> data, HashMap<String, float[]> results, float[] efforResult){
		HierarchicalClusterThread[] HierarchicalClusterServices = new HierarchicalClusterThread[NUMBER_OF_PROCESSORS];
		Thread[] HierarchicalClusterThreads = new Thread[NUMBER_OF_PROCESSORS];
		double width = data.get(this.modelList[0]).getSize()[1];
		double height = data.get(this.modelList[0]).getSize()[0];
		int delta = (int) (height/NUMBER_OF_PROCESSORS);
		NUMBER_OF_PROCESSORS = 1;
		for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
			int h1 = i * delta;
			int h2 = (i+1) * delta;
			int startIndex = (int) (h1 * width);
			int endIndex =  (int) (h2 * width);
			if(i==NUMBER_OF_PROCESSORS-1)
				endIndex = (int) (width*height-1.0);
			startIndex = 0;
			endIndex = (int) (width*height-1.0);
			HierarchicalClusterServices[i] = new HierarchicalClusterThread(startIndex, endIndex, this.modelList, data, efforResult, results);
			HierarchicalClusterThreads[i] = new Thread(HierarchicalClusterServices[i]);
			HierarchicalClusterThreads[i].start();
		}
		try{
			for(int i=0; i<NUMBER_OF_PROCESSORS; i++){
				HierarchicalClusterThreads[i].join();
				System.out.println(i + " Pair Finished~");
			}
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static Cluster createSampleCluster() {
		double[][] distances = new double[][] { 
			    { 0, 1, 9, 7, 11, 14, 13 },
			    { 1, 0, 4, 3, 8, 10, 12 }, 
			    { 9, 4, 0, 9, 2, 8, 10 },
			    { 7, 3, 9, 0, 6, 13, 12 }, 
			    { 11, 8, 2, 6, 0, 10, 20 },
			    { 14, 10, 8, 13, 10, 0, 5 },
			    { 13, 12, 10, 12, 20, 5, 0 }};
	  String[] names = new String[] { "O1", "O2", "O3", "O4", "O5", "O6", "O7" };
	  ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
	  Cluster cluster = alg.performClustering(distances, names, new AverageLinkageStrategy());
	  cluster.toConsole(0);
	  // to compute cluster membership divergence
	  // we can go through every pair of node and find the nearest cluster that contains the pair
	  // and get the size of the cluster(in here it is the weight value)
	  for(int i=0; i<names.length-1; i++){
	  	for(int j=i+1; j<names.length; j++){
	  		double dist = cluster.computeCMDDistance(names[i], names[j]);
	  		System.out.println("Distance between " + names[i] + "," + names[j] + ":" + dist);
	  	}
	  }
	  System.out.println("Total distance is: " + cluster.getTotalDistance());
	  return cluster;
	 
	}
	
	
	public HashMap<String, TiffParser> loadTiffFiles(HashMap<String, File> filePath, String[] keyList){
		HashMap<String, TiffParser> parsers = new HashMap<String, TiffParser>();
		int numOfProcessors = keyList.length;
		LoadTiffFiles[] LoadTiffFilesServices = new LoadTiffFiles[numOfProcessors];
		Thread[] LoadTiffFilesThreads = new Thread[numOfProcessors];
//		int delta = files.size()/9;
		for(int i=0; i<numOfProcessors; i++){
			String key = keyList[i];
			LoadTiffFilesServices[i] = new LoadTiffFiles(key, filePath.get(key), parsers);
			LoadTiffFilesThreads[i] = new Thread(LoadTiffFilesServices[i]);
			LoadTiffFilesThreads[i].start();
		}
		try{
			for(int i=0; i<numOfProcessors; i++){
				LoadTiffFilesThreads[i].join();
//				System.out.println(i + " Loading Finished! " + " Key: " + keyList[i] + " " + parsers.get(keyList[i]).get(0).getFilePath());
			}
		}catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return parsers;
	}
	
//	module for saving tiff files
	public boolean saveTiff(TiffParser template, String outputfile, float[] bufferSet){
		Driver driver = gdal.GetDriverByName("GTiff");
		Dataset dst_ds = driver.Create(outputfile, (int)template.getSize()[1], (int)template.getSize()[0], 1, gdalconst.GDT_Float32);
		dst_ds.SetGeoTransform(template.getGeoInfo());
		dst_ds.SetProjection(template.getProjRef());
		int writingResult = dst_ds.GetRasterBand(1).WriteRaster(0, 0, (int)template.getSize()[1], (int)template.getSize()[0], bufferSet);
		dst_ds.FlushCache();
		dst_ds.delete();
		System.out.println("Writing geotiff result is: " + writingResult);	
		return true;
	}
	
	// module for getting all files within keywords, loop while
	public File getAllFiles(String directoryName, String keyword) {
			    File directory = new File(directoryName);

			    // get all the files from a directory
			    File[] fList = directory.listFiles();
			    for (File file : fList) {
			    	String name = file.getName();
			        if (file.isFile() && name.endsWith(".tif") && name.contains(keyword)) {
			        	return file;
			        }
			    }
			    System.out.println("Cannot find the given file: " + keyword);
			    return null;
		}
}
