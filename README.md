SDN-Traceroute
==============

Tutorial  

>  The traceroute is triggered by REST API: http://x.x.x.x:8080/wm/debugui/traceroute/json.  
>  You should define the traceroute infomation, including ingress switch, ingress port, src/dst, packet characteristic, in the JSON message.  
>        **Check "pktInfo.java" to see all the supported fields.
>   
>  POST two JSON messages (round-trip) to the Controller. (E.g., A->B, B->A)  
>	 And then, 2nd response will contain correct traceroute results.
>	
>	{
>	  "sw":8, 
>	  "inPort":2,
>	  "dataLayerDestination":"00:00:00:00:00:01",
>	  "dataLayerSource":"00:00:00:00:00:08"
>	}  
>	    
>	{
>	  "sw":4, 
>	  "inPort":1,
>	  "dataLayerDestination":"00:00:00:00:00:08",
>	  "dataLayerSource":"00:00:00:00:00:01"
>	}

Reference   

>	[paper](http://colindixon.com/wp-content/uploads/2012/04/sdn-traceroute-hotsdn-2014-final.pdf)  
>	[slides](http://conferences.sigcomm.org/sigcomm/2014/doc/slides/209.pdf/)  

