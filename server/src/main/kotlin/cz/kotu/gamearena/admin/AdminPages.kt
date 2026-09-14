package cz.kotu.gamearena.admin

import cz.kotu.gamearena.ServerConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.title

fun AuthenticationConfig.adminBasicAuthentication(serverConfig: ServerConfig) {
    basic("admin-auth") {
        realm = "Admin Panel"
        validate { credentials ->
            if (credentials.name == serverConfig.adminUsername && credentials.password == serverConfig.adminPassword) {
                UserIdPrincipal(credentials.name)
            } else {
                null
            }
        }
    }
}

fun Route.adminRoutes() {
    authenticate("admin-auth") {
        // Full page shell
        get("/admin") {
            call.respondHtml(HttpStatusCode.OK) {
                head {
                    title("Admin Panel")
                    // Tailwind for clean UI
                    script(src = "https://cdn.tailwindcss.com") {}
                    // HTMX CDN
                    script(src = "https://unpkg.com/htmx.org@2.0.4") {}
                }
                body(classes = "bg-gray-50 p-8") {
                    div(classes = "max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-sm border border-gray-100") {
                        h1(classes = "text-xl font-semibold text-gray-800 mb-4") { +"System Dashboard" }

                        // Interactive button triggering HTMX request
                        button(classes = "px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition") {
                            // HTMX attributes
                            attributes["hx-get"] = "/admin/stats"
                            attributes["hx-target"] = "#stats-container"
                            attributes["hx-swap"] = "innerHTML"

                            +"Refresh Stats"
                        }

                        // Target container for HTMX fragment injection
                        div(classes = "mt-6") {
                            id = "stats-container"
                            p(classes = "text-gray-500") { +"Click the button to load live metrics..." }
                        }
                    }
                }
            }
        }

        // HTMX partial endpoint (returns only HTML fragment, not a full page)
        get("/admin/stats") {
            call.respondHtml(HttpStatusCode.OK) {
                body {
                    div(classes = "grid grid-cols-2 gap-4") {
                        div(classes = "p-4 bg-blue-50 rounded-lg border border-blue-100") {
                            p(classes = "text-xs text-blue-600 font-medium") { +"Active Users" }
                            p(classes = "text-2xl font-bold text-blue-900") { +"1,248" }
                        }
                        div(classes = "p-4 bg-green-50 rounded-lg border border-green-100") {
                            p(classes = "text-xs text-green-600 font-medium") { +"System Health" }
                            p(classes = "text-2xl font-bold text-green-900") { +"Optimal" }
                        }
                    }
                }
            }
        }
    }
}
