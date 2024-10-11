package io.github.thecguygithub.node.commands

import io.github.thecguygithub.api.platforms.PlatformGroupDisplay
import io.github.thecguygithub.api.platforms.PlatformTypes
import io.github.thecguygithub.node.Node
import org.json.JSONArray
import org.json.JSONObject


//class TestCommand : Command("test", "Create a Group") {
//    init {
//        defaultExecution {
//
//            val pGroup = PlatformGroupDisplay("test", "1.0.0", PlatformTypes.PROXY)
//            val platformGroupJson = JSONObject()
//            platformGroupJson.put("name", "test")
//            platformGroupJson.put("version", "1.0.0")
//            platformGroupJson.put("platformType", "PROXY")
//
//            val nodesArray = mutableListOf("tes").toTypedArray()
//            val templatesArray = mutableListOf("tes").toTypedArray()
//
//
//            val nodes = JSONArray()
//            val templates = JSONArray()
//
//            nodesArray.forEach { nodes.put(it) }
//            templatesArray.forEach { templates.put(it) }
//
//
//            val jsonTask = JSONObject()
//            jsonTask.put("name", "TestTask")
//            jsonTask.put("platformGroupDisplay", platformGroupJson)
//            jsonTask.put("nodes", nodes)
//            jsonTask.put("templates", templates)
//            jsonTask.put("maxMemory", 1024)
//            jsonTask.put("staticService", true)
//            jsonTask.put("minOnline", 1)
//            jsonTask.put("maintenance", false)
//
//            Node.instance?.getRC()?.sendMessage(jsonTask.toString(), "testcloud-events-group-create")
//        }
//    }
//}