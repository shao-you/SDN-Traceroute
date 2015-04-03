package net.floodlightcontroller.traceroute;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class DebugUIWebRoutable  implements RestletRoutable {
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/traceroute/json", TracerouteResource.class);//post
        //router.attach("/servicechain/json", ServiceChainResource.class);//post
        router.attach("/swtag/json", SwTagResource.class);//get
        //router.attach("/route/json", GetRouteResource.class);//get
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/debugui";
    }
}
