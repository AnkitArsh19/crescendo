package com.crescendo.apps.mongodb;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey="mongodb", actionKey="insert-one")
public class MongoDbInsertOneHandler extends MongoDbHandler {
    public MongoDbInsertOneHandler(ObjectMapper mapper){super(mapper);}
    @Override
public ActionResult execute(ActionContext c){try{return insertOneDocument(c,jsonMap(c.configuration().get("document")));}catch(Exception e){return ActionResult.failure("MongoDB insert failed: "+e.getMessage());}}
}
