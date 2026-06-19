package com.crescendo.apps.mongodb;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey="mongodb", actionKey="find")
public class MongoDbFindHandler extends MongoDbHandler {
    public MongoDbFindHandler(ObjectMapper mapper){super(mapper);}
    @Override
public ActionResult execute(ActionContext c){try{return findDocuments(c,jsonMap(c.configuration().get("filter")),intVal(c.configuration().get("limit"),20));}catch(Exception e){return ActionResult.failure("MongoDB find failed: "+e.getMessage());}}
}
