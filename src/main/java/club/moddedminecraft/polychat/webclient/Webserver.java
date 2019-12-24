package club.moddedminecraft.polychat.webclient;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import spark.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Webserver {

    VelocityEngine velocityEngine;

    public Webserver() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();
        Template t =velocityEngine.getTemplate("templates/index.html");
        VelocityContext context = new VelocityContext();

        Spark.get("/", (req, res) -> {
            context.put("messages", Main.messages);
            StringWriter writer = new StringWriter();
            t.merge(context, writer);
            return writer.toString();
        });

        Spark.post("/message", (req, res) -> {
            String message = req.queryParams("message");
            Main.sendChat(message);
            res.redirect("/");
            return res;
        });
    }

}
