package test.java.com.apporiented.algorithm.clustering;

import java.util.ArrayList;
import java.util.HashMap;

import main.java.com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.Cluster;
import main.java.com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

public class HierarchicalClusteringTotalDist {
	public class HCComputingThread implements Runnable{
		public int startIndex;
		public int endIndex;
		public HashMap<String, TiffParser> parsers;
		public int[] result;
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
		
		public HCComputingThread(int startIndex, int endIndex, HashMap<String, TiffParser> parsers, int[] result){
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.parsers = parsers;
			this.result = result;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i=this.startIndex; i<this.endIndex; i++){
				String[] names = new String[modelList.length];
				double[][] dist = new double[modelList.length][modelList.length];
				double[][] cmddist = new double[modelList.length][modelList.length];
				for(int m=0; m<modelList.length; m++){
					names[m] = "O"+m;
					for(int n=0; n<modelList.length; n++){
						dist[m][n] = this.parsers.get(modelList[m]+"_"+modelList[n]).getData()[i];
					}
				}
				ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
				Cluster cluster = alg.performClustering(dist, names, new AverageLinkageStrategy());
				String content = null;
				for(int m=0; m<names.length-1; m++){
				  	for(int n=m+1; n<names.length; n++){
				  		double cmdDist = 0;
				  		if(dist[m][n] != 0){
				  			cmddist[m][n] = Double.NaN;
				  		}
				  		else{
				  			cmddist[m][n] = cluster.computeCMDDistance(names[m], names[n]);
				  		}
				  		content += names[m] + "," + names[n] + "," + cmddist[m][n] + "\n";
				  	}
				}
//				this.result[i] = (float) effort;
			}
			
			
		}
		
	}
}
