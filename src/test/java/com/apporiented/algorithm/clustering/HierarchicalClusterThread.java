package test.java.com.apporiented.algorithm.clustering;

import java.util.HashMap;

import main.java.com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.Cluster;
import main.java.com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.CompleteLinkageStrategy;
import main.java.com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import main.java.com.apporiented.algorithm.clustering.SingleLinkageStrategy;

public class HierarchicalClusterThread implements Runnable{
	HashMap<String, TiffParser> data;
	private int startIndex;
	private int endIndex;
	private HashMap<String, float[]> result;
	private String[] keyset;
	private float[] efforResult;
	
	public HierarchicalClusterThread(int startIndex, int endIndex, String[] keyset, HashMap<String, TiffParser> data, float[] efforResult, HashMap<String, float[]> result){
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.data = data;
		this.result = result;
		this.keyset = keyset;
		this.efforResult = efforResult;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		for(int i=this.startIndex; i<this.endIndex; i++){
			if(!Double.isNaN(this.data.get(this.keyset[0]).getData()[i]) && 
					this.data.get(this.keyset[0]).getData()[i] != -1){
				double[][] dist = new double[keyset.length][keyset.length];
				String[] names = new String[keyset.length];
				for(int m=0; m<keyset.length; m++){
					names[m] = "O"+m;
					for(int n=0; n<keyset.length; n++){
						if(!Double.isNaN(this.data.get(keyset[m]).getData()[i]) && this.data.get(keyset[m]).getData()[i] != -1 
								&& !Double.isNaN(this.data.get(keyset[n]).getData()[i]) && this.data.get(keyset[n]).getData()[i] != -1){
							dist[m][n] = Math.abs(this.data.get(keyset[m]).getData()[i] - this.data.get(keyset[n]).getData()[i]);
						}
					}
				}

				ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
				Cluster cluster = alg.performClustering(dist, names, new AverageLinkageStrategy());
				
				int k=0;
				for(int m=0; m<names.length-1; m++){
				  	for(int n=m+1; n<names.length; n++){
				  		double cmdDist = cluster.computeCMDDistance(names[m], names[n]);
//				  		if(m==n && cmdDist>=2)
//				  			cmdDist-=1;
				  		String key = String.valueOf(k);
				  		float[] _result = this.result.get(key);
				  		_result[i] = (float) cmdDist;
				  		this.result.put(key, _result);
				  		k++;
				  	}
				}
				double effort = cluster.getDistanceValue();
				efforResult[i] = (float) effort;
			}
		}
	}

}
