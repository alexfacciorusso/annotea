package com.alexfacciorusso.annotea.processor

import com.alexfacciorusso.annotea.annotation.Action
import com.alexfacciorusso.annotea.annotation.IdeaPlugin
import com.jamesmurty.utils.XMLBuilder2
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation


/**
 * @author alexfacciorusso
 *
 * http://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_configuration_file.html
 */
@SupportedAnnotationTypes("com.alexfacciorusso.annotea.annotation.IdeaPlugin")
class AnnoteaProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val ideaPluginAnnotation = roundEnv.getElementsAnnotatedWith(IdeaPlugin::class.java)
                .firstOrNull()?.getAnnotation(IdeaPlugin::class.java) ?: return true

        val ideaPluginNode = createIdeaPluginRootNode(ideaPluginAnnotation)
        createVendorNode(ideaPluginAnnotation, ideaPluginNode)
        createDependenciesNode(ideaPluginAnnotation, ideaPluginNode)
        createVersionNode(ideaPluginAnnotation, ideaPluginNode)
        createActionsNode(ideaPluginNode, roundEnv.getElementsAnnotatedWith(Action::class.java))

        saveToFile(ideaPluginNode)

        return true
    }

    private fun createIdeaPluginRootNode(ideaPluginAnnotation: IdeaPlugin): XMLBuilder2 {
        return createXml("idea-plugin") {
            attrnb("url", ideaPluginAnnotation.url)
            elemnb("name", ideaPluginAnnotation.name)
            elemnb("id", ideaPluginAnnotation.id)
            elemnb("description", ideaPluginAnnotation.description)
            elemnb("change-notes", ideaPluginAnnotation.changeNotes.text)
            elemnb("version", ideaPluginAnnotation.version)
        }
    }

    private fun createVendorNode(ideaPluginAnnotation: IdeaPlugin, ideaPluginNode: XMLBuilder2) {
        val vendor = ideaPluginAnnotation.vendor
        ideaPluginNode.elemnb("vendor", vendor.name) {
            attrnb("url", vendor.url)
            attrnb("email", vendor.email)
            attrnb("logo", vendor.logo)
        }
    }

    private fun createDependenciesNode(ideaPluginAnnotation: IdeaPlugin, ideaPluginNode: XMLBuilder2) {
        for (dependency in ideaPluginAnnotation.dependencies) {
            ideaPluginNode.elemnb("depends", dependency.name) {
                attrnb("optional", if (dependency.optional) "true" else "false")
                attrnb("config-file", dependency.configFile)
            }
        }
    }

    private fun createVersionNode(ideaPluginAnnotation: IdeaPlugin, ideaPluginNode: XMLBuilder2) {
        val ideaVersion = ideaPluginAnnotation.ideaVersion
        val hasIdeaVersion = ideaVersion.sinceBuild != -1 && ideaVersion.untilBuild != -1
        ideaPluginNode.elemcond("idea-version", hasIdeaVersion) {
            attr("since-build", ideaVersion.sinceBuild.toString())
            attr("until-build", ideaVersion.untilBuild.toString())
        }
    }

    private fun createActionsNode(ideaPluginNode: XMLBuilder2, actionElements: MutableSet<out Element>) {
        ideaPluginNode.elem("actions") {
            for (actionElement in actionElements) {
                val annotation = actionElement.getAnnotation(Action::class.java)

                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Detected action $actionElement")

                //TODO add AnAction type checking

                elem("action") {
                    attr("id", annotation.id)
                    attrnb("description", annotation.description)
                    attrnb("text", annotation.text)
                    attrnb("class", actionElement.toString())

                    for (shortcut in annotation.keyboardShortcuts) {
                        elem("keyboard-shortcut") {
                            attrnb("first-keystroke", shortcut.firstKeystroke)
                            attrnb("second-keystroke", shortcut.secondKeystroke)
                            attrnb("keymap", shortcut.keymap)
                        }
                    }
                }
            }
        }
    }

    private fun saveToFile(ideaPluginNode: XMLBuilder2) {
        val outputProperties = Properties().apply {
            put(javax.xml.transform.OutputKeys.INDENT, "yes")
            put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes")
            put("{http://xml.apache.org/xslt}indent-amount", "2")
        }

        val createResource = processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/plugin.xml")
        ideaPluginNode.toWriter(createResource.openWriter(), outputProperties)
    }
}

fun XMLBuilder2.attrnb(name: String, value: String?) {
    if (value?.isNotBlank() ?: false) this.attr(name, value)
}

fun XMLBuilder2.elemnb(name: String, text: String?, function: (XMLBuilder2.() -> Unit)? = null): XMLBuilder2? {
    return if (text?.isNotBlank() ?: false) {
        elem(name).apply {
            text(text)
            function?.invoke(this)
        }
    } else null
}

fun XMLBuilder2.elem(name: String, function: (XMLBuilder2.() -> Unit)? = null): XMLBuilder2? {
    return elem(name).apply {
        function?.invoke(this)
    }
}

fun XMLBuilder2.elemcond(name: String, condition: Boolean, function: (XMLBuilder2.() -> Unit)? = null): XMLBuilder2? {
    return if (condition) {
        elem(name).apply {
            function?.invoke(this)
        }
    } else null
}

fun createXml(name: String, function: (XMLBuilder2.() -> Unit)? = null): XMLBuilder2 {
    return XMLBuilder2.create(name).apply {
        function?.invoke(this!!)
    }
}