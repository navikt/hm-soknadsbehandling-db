package no.nav.hjelpemidler.soknad.db.safselvbetjening

import io.ktor.resources.Resource

@Resource("/bruker")
class Bruker {
    @Resource("/dokumenter")
    class Dokumenter(val parent: Bruker = Bruker()) {
        @Resource("/{fagsakId}")
        class ForSak(val fagsakId: String, val parent: Dokumenter = Dokumenter())
    }

    @Resource("/arkiv-dokumenter")
    class ArkivDokumenter(val parent: Bruker = Bruker()) {
        @Resource("/{journalpostId}")
        class JournalpostId(val journalpostId: String, val parent: ArkivDokumenter = ArkivDokumenter()) {
            @Resource("/{dokumentId}")
            class DokumentId(val dokumentId: String, val parent: JournalpostId) {
                constructor(journalpostId: String, dokumentId: String) : this(dokumentId, JournalpostId(journalpostId))

                @Resource("/{dokumentvariant}")
                class Dokumentvariant(val dokumentvariant: String, val parent: DokumentId) {
                    constructor(journalpostId: String, dokumentId: String, dokumentvariant: String) : this(dokumentvariant, DokumentId(dokumentId, JournalpostId(journalpostId)))
                }
            }
        }
    }
}
