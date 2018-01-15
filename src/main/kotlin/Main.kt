import spark.kotlin.Http
import spark.kotlin.ignite
import java.io.File
import java.util.*

object Main {

    @JvmStatic fun main(args: Array<String>) {

        val staticFilesLocation = File("html")
        val baseLocation = File(args.firstOrNull() ?: "videos")

        val segments = baseLocation.walkTopDown().maxDepth(1).filter { it.name.endsWith(".txt") }.map {
            val lines = it.readLines()
            return@map Segment(it.nameWithoutExtension + "cue.mp4", lines[0].toInt(), lines[1].toInt())
        }.toList()

        val rand = Random()

        var currentSegment = segments[rand.nextInt(segments.size)]

        val http: Http = ignite()
        http.apply {
            port(12345)
            staticFiles.location("/")
            staticFiles.externalLocation(staticFilesLocation.absolutePath)
        }

        http.options("/*") {

            val accessControlRequestHeaders = request
                    .headers("Access-Control-Request-Headers")
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers",
                        accessControlRequestHeaders)
            }

            val accessControlRequestMethod = request
                    .headers("Access-Control-Request-Method")
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods",
                        accessControlRequestMethod)
            }

            "OK"
        }

        http.before { response.header("Access-Control-Allow-Origin", "*") }

        val submit: spark.kotlin.RouteHandler.() -> Any = submit@ {
            val team = if (this.request.queryParams().contains("team")) {
                this.request.queryParams("team").toIntOrNull()
            } else {
                null
            }
            val video = if (this.request.queryParams().contains("video")) {
                this.request.queryParams("video").toIntOrNull()
            } else {
                null
            }
            val frame = if (this.request.queryParams().contains("frame")) {
                this.request.queryParams("frame").toIntOrNull()
            } else {
                null
            }
            val sequence = if(this.request.queryParams().contains("iseq")){
                this.request.queryParams("iseq")
            }else{
                null
            }

            var returnString = ""

            returnString += when{
                team == null -> " Missing team id."
                team < 0 -> " Invalid team id ($team)."
                else -> ""
            }

            returnString += when{
                video == null -> " Missing video id."
                video != currentSegment.id -> " Wrong video id ($video)."
                else -> ""
            }

            returnString += when{
                frame == null -> " No frame specified."
                frame < currentSegment.startFrame || frame > currentSegment.endFrame -> " Wong frame number ($frame)."
                else -> ""
            }

            if (returnString.isBlank()){
                returnString = "Correct!"
            }

            if(sequence == null || segments.isEmpty()){
                returnString += " Please also include your sequence of actions for a detailed analysis!"
            }

            return@submit returnString
        }


        http.get("/submit", function = submit)
        http.post("/submit", function = submit)

        http.get("/video") {
            return@get File(baseLocation, currentSegment.name).inputStream().copyTo(response.raw().outputStream)
        }

        http.get("/next") {
            currentSegment = segments[rand.nextInt(segments.size)]
            return@get currentSegment.toString()
        }

        println(currentSegment)


        while (true) {
            val line = readLine()
            if (line?.equals("quit") != false) {
                http.stop()
                break
            } else {
                currentSegment = segments[rand.nextInt(segments.size)]
                println(currentSegment)
            }
        }

    }

}
data class Segment(val name: String, val startFrame: Int, val endFrame: Int) {

    val id = name.substringAfter("shot").substringBefore("_").toInt()

}