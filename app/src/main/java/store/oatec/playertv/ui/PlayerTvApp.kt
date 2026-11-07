package store.oatec.playertv.ui

import store.oatec.playertv.BuildConfig

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.os.Build
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.KeyEvent as AndroidKeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.MimeTypes
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.getOrThrow
import io.github.yuroyami.libmpvkt.view.MpvOptions
import io.github.yuroyami.libmpvkt.view.PreparedCore
import io.github.yuroyami.libmpvkt.view.MpvView
import coil.compose.AsyncImage
import java.io.File
import java.io.IOException
import java.util.UUID
import org.json.JSONArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import store.oatec.playertv.data.PlayerPreferences
import store.oatec.playertv.data.OatecAboutRepository
import store.oatec.playertv.data.SecureSourceStore
import store.oatec.playertv.data.SourceRepository
import store.oatec.playertv.data.MediaCatalogStore
import store.oatec.playertv.data.SourceCategory
import store.oatec.playertv.data.OfflineDownloadStore
import store.oatec.playertv.data.OfflineItem
import store.oatec.playertv.data.EpgGuideClient
import store.oatec.playertv.data.EpgProgram
import store.oatec.playertv.data.EpgRepository
import store.oatec.playertv.data.KinoCheckClient
import store.oatec.playertv.data.KinoCheckVideo
import store.oatec.playertv.model.MediaEntry
import store.oatec.playertv.model.MediaKind
import store.oatec.playertv.model.CatalogSection
import store.oatec.playertv.model.PlayerSource
import store.oatec.playertv.model.SourceCatalog
import store.oatec.playertv.model.SourceType

private val LocalAppLanguage = staticCompositionLocalOf { "en" }

private fun appText(source: String, language: String): String {
    val translations = mapOf(
        "Set / change PIN" to listOf("تعيين / تغيير الرمز", "Définir / modifier le PIN", "Set / change PIN", "Configurar / cambiar PIN", "PIN festlegen / ändern", "PIN instellen / wijzigen", "Ställ in / ändra PIN", "PIN setze / ändere", "Imposta / cambia PIN", "Definir / midar il PIN"),
        "Create PIN" to listOf("إنشاء رمز", "Créer un PIN", "Create PIN", "Crear PIN", "PIN erstellen", "PIN aanmaken", "Skapa PIN", "PIN erstelle", "Crea PIN", "Crear PIN"),
        "Set a new Kids Mode PIN" to listOf("تعيين رمز جديد لوضع الأطفال", "Définir un nouveau PIN du mode enfants", "Set a new Kids Mode PIN", "Configura un nuevo PIN del modo infantil", "Neue Kindermodus-PIN festlegen", "Nieuwe PIN voor kindermodus instellen", "Ange en ny PIN för barnläget", "Neue PIN fürs Chindermodus setze", "Imposta un nuovo PIN modalità bambini", "Definir in nov PIN per il modus d’uffants"),
        "Create Kids Mode PIN" to listOf("إنشاء رمز وضع الأطفال", "Créer le PIN du mode enfants", "Create Kids Mode PIN", "Crear PIN del modo infantil", "Kindermodus-PIN erstellen", "PIN voor kindermodus aanmaken", "Skapa PIN för barnläget", "PIN fürs Chindermodus erstelle", "Crea PIN modalità bambini", "Crear PIN dal modus d’uffants"),
        "Settings" to listOf("الإعدادات", "Paramètres", "Settings", "Ajustes", "Einstellungen", "Instellingen", "Inställningar", "Iistellige", "Impostazioni", "Impostaziuns"),
        "Add an IPTV source to begin" to listOf("أضف مصدراً لـ IPTV للبدء", "Ajoutez une source IPTV pour commencer", "Add an IPTV source to begin", "Añade una fuente IPTV para comenzar", "Füge eine IPTV-Quelle hinzu", "Voeg een IPTV-bron toe om te beginnen", "Lägg till en IPTV-källa för att börja", "Füeg e IPTV-Quelle dezue", "Aggiungi una fonte IPTV per iniziare", "Agiunta ina funtauna IPTV per cumenzar"),
        "Load" to listOf("تحميل", "Charger", "Load", "Cargar", "Laden", "Laden", "Ladda", "Lade", "Carica", "Chargiar"),
        "Refresh" to listOf("تحديث", "Actualiser", "Refresh", "Actualizar", "Aktualisieren", "Vernieuwen", "Uppdatera", "Aktualisiere", "Aggiorna", "Actualisar"),
        "Add" to listOf("إضافة", "Ajouter", "Add", "Añadir", "Hinzufügen", "Toevoegen", "Lägg till", "Dezuefüege", "Aggiungi", "Agiuntar"),
        "Add source" to listOf("إضافة مصدر", "Ajouter une source", "Add source", "Añadir fuente", "Quelle hinzufügen", "Bron toevoegen", "Lägg till källa", "Quelle dezuefüege", "Aggiungi fonte", "Agiuntar funtauna"),
        "Delete" to listOf("حذف", "Supprimer", "Delete", "Eliminar", "Löschen", "Verwijderen", "Ta bort", "Lösche", "Elimina", "Stizzar"),
        "Trailer preview unavailable" to listOf("معاينة الإعلان غير متاحة", "Aperçu de la bande-annonce indisponible", "Trailer preview unavailable", "Vista previa del tráiler no disponible", "Trailer-Vorschau nicht verfügbar", "Trailerpreview niet beschikbaar", "Förhandsvisning av trailer inte tillgänglig", "Trailer-Vorschau nid verfüegbar", "Anteprima trailer non disponibile", "Prevista dal trailer betg disponibla"),
        "Searching trailer…" to listOf("جارٍ البحث عن الإعلان…", "Recherche de la bande-annonce…", "Searching trailer…", "Buscando tráiler…", "Trailer wird gesucht…", "Trailer zoeken…", "Söker trailer…", "Sueche Trailer…", "Ricerca trailer…", "Tschertga trailer…"),
        "Find trailer" to listOf("البحث عن الإعلان", "Rechercher la bande-annonce", "Find trailer", "Buscar tráiler", "Trailer suchen", "Trailer zoeken", "Sök trailer", "Trailer sueche", "Cerca trailer", "Tschertgar trailer"),
        "Episodes" to listOf("الحلقات", "Épisodes", "Episodes", "Episodios", "Folgen", "Afleveringen", "Avsnitt", "Folge", "Episodi", "Episodis"),
        "← Back to series" to listOf("← الرجوع للمسلسلات", "← Retour aux séries", "← Back to series", "← Volver a series", "← Zurück zu Serien", "← Terug naar series", "← Tillbaka till serier", "← Zrugg zu Serie", "← Torna alle serie", "← Turnar a las serias"),
        "Play" to listOf("تشغيل", "Lire", "Play", "Reproducir", "Abspielen", "Afspelen", "Spela", "Abspiele", "Riproduci", "Reproducir"),
        "Favorite" to listOf("المفضلة", "Favori", "Favorite", "Favorito", "Favorit", "Favoriet", "Favorit", "Favorit", "Preferito", "Preferì"),
        "Saved" to listOf("تم الحفظ", "Enregistré", "Saved", "Guardado", "Gespeichert", "Opgeslagen", "Sparat", "Gspeicheret", "Salvato", "Memorisà"),
        "Enter a channel number" to listOf("أدخل رقم القناة", "Entrez un numéro de chaîne", "Enter a channel number", "Introduce un número de canal", "Kanalnummer eingeben", "Voer een kanaalnummer in", "Ange ett kanalnummer", "Kanalnummer iigeh", "Inserisci il numero del canale", "Endatescha il numer dal chanàl"),
        "No channel with that number" to listOf("لا توجد قناة بهذا الرقم", "Aucune chaîne avec ce numéro", "No channel with that number", "No hay ningún canal con ese número", "Kein Sender mit dieser Nummer", "Geen kanaal met dat nummer", "Ingen kanal med det numret", "Kei Sender mit dere Nummer", "Nessun canale con questo numero", "Nagina chanàl cun quest numer"),
        "Channels" to listOf("القنوات", "Chaînes", "Channels", "Canales", "Kanäle", "Kanalen", "Kanaler", "Kanäl", "Canali", "Chanals"),
        "Hide channels" to listOf("إخفاء القنوات", "Masquer les chaînes", "Hide channels", "Ocultar canales", "Kanäle ausblenden", "Kanalen verbergen", "Dölj kanaler", "Kanäl verstecke", "Nascondi canali", "Zuppentar chanals"),
        "Resume" to listOf("استئناف", "Reprendre", "Resume", "Reanudar", "Fortsetzen", "Hervatten", "Återuppta", "Fortsetze", "Riprendi", "Cuntinuar"),
        "Pause" to listOf("إيقاف مؤقت", "Pause", "Pause", "Pausar", "Pause", "Pauzeren", "Pausa", "Pause", "Pausa", "Pausa"),
        "Try default player" to listOf("تجربة المشغل الافتراضي", "Essayer le lecteur par défaut", "Try default player", "Probar reproductor predeterminado", "Standardplayer testen", "Standaardspeler proberen", "Prova standardspelaren", "Standardplayer teste", "Prova il lettore predefinito", "Empruvar il player standard"),
        "Try MPV" to listOf("تجربة MPV", "Essayer MPV", "Try MPV", "Probar MPV", "MPV testen", "MPV proberen", "Prova MPV", "MPV teste", "Prova MPV", "Empruvar MPV"),
        "Quality not identified" to listOf("الجودة غير محددة", "Qualité non identifiée", "Quality not identified", "Calidad no identificada", "Qualität nicht erkannt", "Kwaliteit niet herkend", "Kvalitet okänd", "Qualität nid erkannt", "Qualità non identificata", "Qualitad betg identifitgada"),
        "Official trailer" to listOf("الإعلان الرسمي", "Bande-annonce officielle", "Official trailer", "Tráiler oficial", "Offizieller Trailer", "Officiële trailer", "Officiell trailer", "Offizielle Trailer", "Trailer ufficiale", "Trailer uffizial"),
        "🔥 Trending" to listOf("🔥 الرائج","🔥 Tendances","🔥 Trending","🔥 Tendencias","🔥 Im Trend","🔥 Trending","🔥 Trendar","🔥 Im Trend","🔥 Tendenze","🔥 Tendenzas"),
        "⭐ Recommended" to listOf("⭐ مقترح لك","⭐ Recommandé","⭐ Recommended","⭐ Recomendado","⭐ Empfohlen","⭐ Aanbevolen","⭐ Rekommenderat","⭐ Empfohle","⭐ Consigliati","⭐ Recumandà"),
        "All" to listOf("الكل","Tout","All","Todo","Alle","Alles","Alla","Alli","Tutti","Tuts"),
        "Favorites" to listOf("المفضلة","Favoris","Favorites","Favoritos","Favoriten","Favorieten","Favoriter","Favoritä","Preferiti","Favurits"),
        "Hidden" to listOf("المخفية","Masquées","Hidden","Ocultos","Ausgeblendet","Verborgen","Dolda","Versteckt","Nascosti"," zuppentads"),
        "Sports" to listOf("رياضة","Sports","Sports","Deportes","Sport","Sport","Sport","Sport","Sport","Sport"),
        "News" to listOf("أخبار","Actualités","News","Noticias","Nachrichten","Nieuws","Nyheter","Nachrichte","Notizie","Novitads"),
        "Entertainment" to listOf("ترفيه","Divertissement","Entertainment","Entretenimiento","Unterhaltung","Amusement","Underhållning","Unterhaltig","Intrattenimento","Divertiment"),
        "Kids" to listOf("أطفال","Enfants","Kids","Infantil","Kinder","Kinderen","Barn","Chind","Bambini","Uffants"),
        "Premium code (optional)" to listOf("رمز Premium (اختياري)","Code Premium (facultatif)","Premium code (optional)","Código Premium (opcional)","Premium-Code (optional)","Premium-code (optioneel)","Premiumkod (valfritt)","Premium-Code (optional)","Codice Premium (facoltativo)","Code Premium (opziunal)"),
        "The code is stored securely. Online verification is not available until OATEC provides an activation API." to listOf("يتم حفظ الرمز بشكل آمن. التحقق عبر الإنترنت غير متاح حتى توفر OATEC واجهة التفعيل.","Le code est stocké en toute sécurité. La vérification en ligne sera disponible lorsque OATEC fournira une API d’activation.","The code is stored securely. Online verification is not available until OATEC provides an activation API.","El código se guarda de forma segura. La verificación en línea estará disponible cuando OATEC proporcione una API de activación.","Der Code wird sicher gespeichert. Eine Online-Prüfung ist erst möglich, wenn OATEC eine Aktivierungs-API bereitstellt.","De code wordt veilig opgeslagen. Onlineverificatie is pas mogelijk wanneer OATEC een activerings-API aanbiedt.","Koden lagras säkert. Onlineverifiering blir tillgänglig när OATEC tillhandahåller ett aktiverings-API.","De Code wird sicher gspeicheret. Online-Prüefig isch erst möglich, wenn OATEC e Aktivierungs-API bereitstellt.","Il codice è salvato in modo sicuro. La verifica online sarà disponibile quando OATEC fornirà un’API di attivazione.","Il code vegn memorisà en moda segira. La verificaziun online è disponibla cura che OATEC porscha in’API d’activaziun."),
        "Loading channels" to listOf("جارٍ تحميل القنوات","Chargement des chaînes","Loading channels","Cargando canales","Kanäle werden geladen","Kanalen laden","Laddar kanaler","Kanäl werde glade","Caricamento canali","Chargiar chanals"),
        "Please wait until the catalogue is ready" to listOf("يرجى الانتظار حتى يصبح الكتالوغ جاهزاً","Veuillez patienter jusqu’à ce que le catalogue soit prêt","Please wait until the catalogue is ready","Espera hasta que el catálogo esté listo","Bitte warten, bis der Katalog bereit ist","Wacht tot de catalogus klaar is","Vänta tills katalogen är klar","Bitte warte, bis de Katalog parat isch","Attendi che il catalogo sia pronto","Spetga fin che il catalog è pront"),
        "items loaded" to listOf("عنصر تم تحميله","éléments chargés","items loaded","elementos cargados","Elemente geladen","items geladen","objekt laddade","Element glade","elementi caricati","elements chargiads"),
        "About OATEC" to listOf("حول OATEC","À propos d’OATEC","About OATEC","Acerca de OATEC","Über OATEC","Over OATEC","Om OATEC","Über OATEC","Informazioni su OATEC","Davart OATEC"),
        "DOWNLOADS" to listOf("التنزيلات","TÉLÉCHARGEMENTS","DOWNLOADS","DESCARGAS","DOWNLOADS","DOWNLOADS","NEDLADDNINGAR","DOWNLOADS","DOWNLOAD","TELECARGIADAS"),
        "Home" to listOf("الرئيسية","Accueil","Home","Inicio","Startseite","Home","Hem","Start","Home","Mussavia"),
        "Live TV" to listOf("التلفاز المباشر","TV en direct","Live TV","TV en directo","Live-TV","Live-tv","Direkt-TV","Live-TV","TV in diretta","TV directa"),
        "Movies" to listOf("الأفلام","Films","Movies","Películas","Filme","Films","Filmer","Film","Film","Films"),
        "Series" to listOf("المسلسلات","Séries","Series","Series","Serien","Series","Serier","Serien","Serie","Serias"),
        "Trailers" to listOf("الإعلانات","Bandes-annonces","Trailers","Tráilers","Trailer","Trailers","Trailrar","Trailer","Trailer","Trailer"),
        "Downloads" to listOf("التنزيلات","Téléchargements","Downloads","Descargas","Downloads","Downloads","Nedladdningar","Downloads","Download","Telechargiadas"),
        "Sources" to listOf("المصادر","Sources","Sources","Fuentes","Quellen","Bronnen","Källor","Quellen","Sorgenti","Funtanas"),
        "Settings" to listOf("الإعدادات","Paramètres","Settings","Ajustes","Einstellungen","Instellingen","Inställningar","Einstellungen","Impostazioni","Impostaziuns"),
        "← Back" to listOf("رجوع ←","← Retour","← Back","← Volver","← Zurück","← Terug","← Tillbaka","← Zurück","← Indietro","← Enavos"),
        "Exit" to listOf("خروج","Quitter","Exit","Salir","Beenden","Afsluiten","Avsluta","Beenden","Esci","Sortir"),
        "SETTINGS" to listOf("الإعدادات","PARAMÈTRES","SETTINGS","AJUSTES","EINSTELLUNGEN","INSTELLINGEN","INSTÄLLNINGAR","EINSTELLUNGEN","IMPOSTAZIONI","IMPOSTAZIUNS"),
        "Playback, sources and OATEC preferences" to listOf("إعدادات التشغيل والمصادر وOATEC","Lecture, sources et préférences OATEC","Playback, sources and OATEC preferences","Reproducción, fuentes y preferencias de OATEC","Wiedergabe, Quellen und OATEC-Einstellungen","Afspelen, bronnen en OATEC-voorkeuren","Uppspelning, källor och OATEC-inställningar","Wiedergabe, Quellen und OATEC-Einstellungen","Riproduzione, sorgenti e preferenze OATEC","Reproducziun, funtaunas e preferenzas OATEC"),
        "APP LANGUAGE" to listOf("لغة التطبيق","LANGUE DE L’APPLICATION","APP LANGUAGE","IDIOMA DE LA APLICACIÓN","APP-SPRACHE","APP-TAAL","APPSPRÅK","APP-SPRACHE","LINGUA DELL’APP","LINGUA DA L’APP"),
        "Choose the language used by the app." to listOf("اختار لغة التطبيق.","Choisissez la langue de l’application.","Choose the language used by the app.","Elige el idioma de la aplicación.","Wähle die Sprache der App.","Kies de taal van de app.","Välj språk för appen.","Wähle d Sprach vo de App.","Scegli la lingua dell’app.","Tscherna la lingua da l’app."),
        "Language preference is saved on this device." to listOf("سيتم حفظ اللغة على هذا الجهاز.","La langue est enregistrée sur cet appareil.","Language preference is saved on this device.","El idioma se guarda en este dispositivo.","Die Sprache wird auf diesem Gerät gespeichert.","De taal wordt op dit apparaat opgeslagen.","Språket sparas på den här enheten.","D Sprach wird uf dem Gerät gspeicheret.","La lingua viene salvata su questo dispositivo.","La lingua vegn memorisada sin quest apparat."),
        "Sources & playlists" to listOf("المصادر وقوائم التشغيل","Sources et listes de lecture","Sources & playlists","Fuentes y listas","Quellen und Wiedergabelisten","Bronnen en afspeellijsten","Källor och spellistor","Quellen und Playlistene","Sorgenti e playlist","Funtaunas e glistas da reproducziun"),
        "Manage sources" to listOf("إدارة المصادر","Gérer les sources","Manage sources","Gestionar fuentes","Quellen verwalten","Bronnen beheren","Hantera källor","Quellen verwalte","Gestisci sorgenti","Administrar funtaunas"),
        "PLAYBACK" to listOf("التشغيل","LECTURE","PLAYBACK","REPRODUCCIÓN","WIEDERGABE","AFSPELEN","UPPSPELNING","WIEDERGABE","RIPRODUZIONE","REPRODUCZIUN"),
        "Resume VOD playback" to listOf("متابعة مشاهدة الأفلام","Reprendre la lecture VOD","Resume VOD playback","Reanudar reproducción VOD","VOD-Wiedergabe fortsetzen","VOD-weergave hervatten","Återuppta VOD-uppspelning","VOD-Wiedergabe fortsetze","Riprendi riproduzione VOD","Cuntinuar la reproducziun VOD"),
        "Playback engine" to listOf("محرك التشغيل","Moteur de lecture","Playback engine","Motor de reproducción","Wiedergabe-Engine","Afspeelengine","Uppspelningsmotor","Wiedergabe-Engine","Motore di riproduzione","Motur da reproducziun"),
        "Watch history" to listOf("سجل المشاهدة","Historique de visionnage","Watch history","Historial","Wiedergabeverlauf","Kijkgeschiedenis","Visningshistorik","Verlauf","Cronologia","Istorgia da guardar"),
        "Clear history" to listOf("مسح السجل","Effacer l’historique","Clear history","Borrar historial","Verlauf löschen","Geschiedenis wissen","Rensa historik","Verlauf lösche","Cancella cronologia","Stizzar l’istorgia"),
        "KIDS MODE" to listOf("وضع الأطفال","MODE ENFANTS","KIDS MODE","MODO INFANTIL","KINDERMODUS","KINDERMODUS","BARNLÄGE","CHINDERMODUS","MODALITÀ BAMBINI","MODUS D’UFFANTS"),
        "Parent PIN required" to listOf("رمز الوالدين مطلوب","Code parental requis","Parent PIN required","Se requiere PIN parental","Eltern-PIN erforderlich","Ouder-PIN vereist","Föräldra-PIN krävs","Eltere-PIN nötig","PIN genitore richiesto","PIN dals geniturs necessari"),
        "Enter the parent PIN to leave Kids Mode." to listOf("أدخل رمز الوالدين للخروج من وضع الأطفال.","Saisissez le code parental pour quitter le mode enfants.","Enter the parent PIN to leave Kids Mode.","Introduce el PIN parental para salir del modo infantil.","Gib die Eltern-PIN ein, um den Kindermodus zu verlassen.","Voer de ouder-PIN in om de kindermodus te verlaten.","Ange föräldra-PIN för att lämna barnläget.","Gib de Eltere-PIN ii zum Chindermodus z'verlah.","Inserisci il PIN genitore per uscire dalla modalità bambini.","Endatescha il PIN dals geniturs per sortir dal modus d'uffants."),
        "Parent PIN" to listOf("رمز الوالدين","Code parental","Parent PIN","PIN parental","Eltern-PIN","Ouder-PIN","Föräldra-PIN","Eltere-PIN","PIN genitore","PIN dals geniturs"),
        "Exit Kids Mode" to listOf("الخروج من وضع الأطفال","Quitter le mode enfants","Exit Kids Mode","Salir del modo infantil","Kindermodus verlassen","Kindermodus verlaten","Avsluta barnläget","Chindermodus verlah","Esci dalla modalità bambini","Sortir dal modus d'uffants"),
        "Cancel" to listOf("إلغاء","Annuler","Cancel","Cancelar","Abbrechen","Annuleren","Avbryt","Abbräche","Annulla","Annullar"),
        "Premium code" to listOf("رمز Premium","Code Premium","Premium code","Código Premium","Premium-Code","Premium-code","Premiumkod","Premium-Code","Codice Premium","Code Premium"),
        "OATEC website" to listOf("موقع OATEC","Site OATEC","OATEC website","Sitio web de OATEC","OATEC-Website","OATEC-website","OATEC-webbplats","OATEC-Websiite","Sito web OATEC","Website OATEC"),
        "Parental PIN protection" to listOf("حماية برمز الوالدين","Protection par code parental","Parental PIN protection","Protección con PIN parental","Eltern-PIN-Schutz","Bescherming met ouder-PIN","Skydd med föräldra-PIN","Schutz mit Eltere-PIN","Protezione con PIN genitore","Protecziun cun PIN dals geniturs"),
        "Manage content rules" to listOf("إدارة قواعد المحتوى","Gérer les règles de contenu","Manage content rules","Gestionar reglas de contenido","Inhaltsregeln verwalten","Inhoudsregels beheren","Hantera innehållsregler","Inhaltsregle verwalte","Gestisci regole dei contenuti","Administrar reglas dal cuntegn"),
        "Remove PIN" to listOf("إزالة الرمز","Supprimer le code","Remove PIN","Eliminar PIN","PIN entfernen","PIN verwijderen","Ta bort PIN","PIN entferne","Rimuovi PIN","Allontanar PIN"),
        "PIN saved" to listOf("تم حفظ الرمز","Code enregistré","PIN saved","PIN guardado","PIN gespeichert","PIN opgeslagen","PIN sparad","PIN gspeicheret","PIN salvato","PIN memorisà"),
        "Remove Kids Mode PIN?" to listOf("هل تريد إزالة رمز وضع الأطفال؟","Supprimer le code du mode enfants ?","Remove Kids Mode PIN?","¿Eliminar el PIN del modo infantil?","Kindermodus-PIN entfernen?","PIN voor kindermodus verwijderen?","Ta bort PIN för barnläget?","PIN vom Chindermodus entferne?","Rimuovere il PIN della modalità bambini?","Allontanar il PIN dal modus d'uffants?"),
        "Current PIN" to listOf("الرمز الحالي","Code actuel","Current PIN","PIN actual","Aktuelle PIN","Huidige PIN","Nuvarande PIN","Aktueller PIN","PIN attuale","PIN actual"),
        "Allow" to listOf("سماح","Autoriser","Allow","Permitir","Erlauben","Toestaan","Tillåt","Erlaube","Consenti","Permetter"),
        "Block" to listOf("حظر","Bloquer","Block","Bloquear","Blockieren","Blokkeren","Blockera","Sperre","Blocca","Bloccar"),
        "Clear rule" to listOf("مسح القاعدة","Effacer la règle","Clear rule","Borrar regla","Regel löschen","Regel wissen","Rensa regel","Regel lösche","Cancella regola","Stizzar regla"),
        "Close" to listOf("إغلاق","Fermer","Close","Cerrar","Schließen","Sluiten","Stäng","Schliesse","Chiudi","Serrar"),
        "Save PIN" to listOf("حفظ الرمز","Enregistrer le code","Save PIN","Guardar PIN","PIN speichern","PIN opslaan","Spara PIN","PIN speichere","Salva PIN","Memorisar PIN"),
        "Save code" to listOf("حفظ الرمز","Enregistrer le code","Save code","Guardar código","Code speichern","Code opslaan","Spara kod","Code speichere","Salva codice","Memorisar code"),
        "Open OATEC website" to listOf("فتح موقع OATEC","Ouvrir le site OATEC","Open OATEC website","Abrir sitio OATEC","OATEC-Website öffnen","OATEC-website openen","Öppna OATEC-webbplatsen","OATEC-Websiite öffne","Apri il sito OATEC","Avrir il website OATEC"),
        "Saved" to listOf("تم الحفظ","Enregistré","Saved","Guardado","Gespeichert","Opgeslagen","Sparat","Gspeicheret","Salvato","Memorisà"),
        "Loading information from oatec.store" to listOf("جارٍ تحميل المعلومات من oatec.store","Chargement des informations depuis oatec.store","Loading information from oatec.store","Cargando información desde oatec.store","Informationen von oatec.store werden geladen","Informatie laden van oatec.store","Laddar information från oatec.store","Informatione vo oatec.store lade","Caricamento informazioni da oatec.store","Chargiar infurmaziuns da oatec.store"),
        "Refresh info" to listOf("تحديث المعلومات","Actualiser les infos","Refresh info","Actualizar información","Infos aktualisieren","Info vernieuwen","Uppdatera info","Info aktualisiere","Aggiorna info","Actualisar infurmaziuns"),
        "Visit website" to listOf("زيارة الموقع","Visiter le site","Visit website","Visitar sitio web","Website besuchen","Website bezoeken","Besök webbplatsen","Website bsueche","Visita il sito","Visitar il website"),
        "Exit app" to listOf("الخروج من التطبيق","Quitter l’application","Exit app","Salir de la aplicación","App beenden","App afsluiten","Avsluta appen","App verlah","Esci dall’app","Sortir da l'app"),
        "Clear watch history?" to listOf("هل تريد مسح سجل المشاهدة؟","Effacer l’historique de visionnage ?","Clear watch history?","¿Borrar el historial de reproducción?","Wiedergabeverlauf löschen?","Kijkgeschiedenis wissen?","Rensa visningshistoriken?","Verlauf lösche?","Cancellare la cronologia?","Stizzar l'istorgia da guardar?"),
        "Clear" to listOf("مسح","Effacer","Clear","Borrar","Löschen","Wissen","Rensa","Lösche","Cancella","Stizzar"),
        "Search" to listOf("بحث","Rechercher","Search","Buscar","Suchen","Zoeken","Sök","Sueche","Cerca","Tschertgar"),
        "Synopsis" to listOf("القصة","Synopsis","Synopsis","Sinopsis","Zusammenfassung","Samenvatting","Synopsis","Zämefassig","Sinossi","Sinopsis"),
        "Play" to listOf("تشغيل","Lire","Play","Reproducir","Abspielen","Afspelen","Spela","Abspiele","Riproduci","Reproducir"),
        "Back" to listOf("رجوع","Retour","Back","Volver","Zurück","Terug","Tillbaka","Zrugg","Indietro","Enavos"),
        "Your IPTV sources" to listOf("مصادر IPTV الخاصة بك","Vos sources IPTV","Your IPTV sources","Tus fuentes IPTV","Deine IPTV-Quellen","Je IPTV-bronnen","Dina IPTV-källor","Dini IPTV-Quälle","Le tue sorgenti IPTV","Tes funtaunas IPTV"),
        "Import M3U file" to listOf("استيراد ملف M3U","Importer un fichier M3U","Import M3U file","Importar archivo M3U","M3U-Datei importieren","M3U-bestand importeren","Importera M3U-fil","M3U-Datei importiere","Importa file M3U","Importar datoteca M3U"),
        "Add source" to listOf("إضافة مصدر","Ajouter une source","Add source","Añadir fuente","Quelle hinzufügen","Bron toevoegen","Lägg till källa","Quälle hinzuefüege","Aggiungi sorgente","Agiuntar funtauna"),
        "Refresh" to listOf("تحديث","Actualiser","Refresh","Actualizar","Aktualisieren","Vernieuwen","Uppdatera","Aktualisiere","Aggiorna","Actualisar"),
        "Remove" to listOf("إزالة","Supprimer","Remove","Eliminar","Entfernen","Verwijderen","Ta bort","Entferne","Rimuovi","Allontanar"),
        "Add IPTV source" to listOf("إضافة مصدر IPTV","Ajouter une source IPTV","Add IPTV source","Añadir fuente IPTV","IPTV-Quelle hinzufügen","IPTV-bron toevoegen","Lägg till IPTV-källa","IPTV-Quälle hinzuefüege","Aggiungi sorgente IPTV","Agiuntar funtauna IPTV"),
        "Source name" to listOf("اسم المصدر","Nom de la source","Source name","Nombre de la fuente","Quellenname","Bronnaam","Källnamn","Quälle-Name","Nome sorgente","Num da la funtauna"),
        "Save" to listOf("حفظ","Enregistrer","Save","Guardar","Speichern","Opslaan","Spara","Speichere","Salva","Memorisar"),
        "Tune" to listOf("ضبط","Régler","Tune","Sintonizar","Einstellen","Afstemmen","Ställ in","Iistelle","Sintonizza","Sintonisar"),
        "Audio tracks" to listOf("المسارات الصوتية","Pistes audio","Audio tracks","Pistas de audio","Audiospuren","Audiotracks","Ljudspår","Tonspure","Tracce audio","Tractas audio"),
        "Online subtitles" to listOf("ترجمة عبر الإنترنت","Sous-titres en ligne","Online subtitles","Subtítulos en línea","Online-Untertitel","Online-ondertitels","Undertexter online","Online-Untertitel","Sottotitoli online","Suttitels online"),
        "Load subtitles" to listOf("تحميل الترجمة","Charger les sous-titres","Load subtitles","Cargar subtítulos","Untertitel laden","Ondertitels laden","Ladda undertexter","Untertitel lade","Carica sottotitoli","Chargiar suttitels"),
        "Choose quality" to listOf("اختيار الجودة","Choisir la qualité","Choose quality","Elegir calidad","Qualität wählen","Kwaliteit kiezen","Välj kvalitet","Qualität wähle","Scegli qualità","Tscherner qualitad"),
        "Stop" to listOf("إيقاف","Arrêter","Stop","Detener","Stopp","Stoppen","Stoppa","Stoppe","Ferma","Stoppar"),
        "Player settings" to listOf("إعدادات المشغل","Paramètres du lecteur","Player settings","Ajustes del reproductor","Player-Einstellungen","Spelerinstellingen","Spelarinställningar","Player-Iistellige","Impostazioni lettore","Preferenzas dal player"),
        "Retry" to listOf("إعادة المحاولة","Réessayer","Retry","Reintentar","Erneut versuchen","Opnieuw proberen","Försök igen","Nomol probiere","Riprova","Empruvar danovamain"),
        "إضافة قناة يدوياً" to listOf("العربية", "Add channel manually", "Ajouter une chaîne manuellement", "Añadir canal manualmente", "Kanal manuell hinzufügen", "Kanaal handmatig toevoegen", "Lägg till kanal manuellt", "Kanal manuell hinzuefüege", "Aggiungi canale manualmente", "Agiuntar in chanagl manualmain"),
        "اسم القناة" to listOf("العربية", "Channel name", "Nom de la chaîne", "Nombre del canal", "Kanalname", "Kanaalnaam", "Kanalnamn", "Kanalname", "Nome del canale", "Num dal chanagl"),
        "رابط البث" to listOf("العربية", "Stream URL", "URL du flux", "URL de transmisión", "Stream-URL", "Stream-URL", "Strömningsadress", "Stream-URL", "URL dello streaming", "URL dal stream"),
        "رابط الشعار (اختياري)" to listOf("العربية", "Logo URL (optional)", "URL du logo (facultatif)", "URL del logotipo (opcional)", "Logo-URL (optional)", "Logo-URL (optioneel)", "Logotypens URL (valfritt)", "Logo-URL (optional)", "URL del logo (facoltativo)", "URL dal logo (opziunal)"),
        "التصنيف" to listOf("العربية", "Category", "Catégorie", "Categoría", "Kategorie", "Categorie", "Kategori", "Kategorie", "Categoria", "Categoria"),
        "إضافة" to listOf("العربية", "Add", "Ajouter", "Añadir", "Hinzufügen", "Toevoegen", "Lägg till", "Hinzuefüege", "Aggiungi", "Agiuntar"),
        "إلغاء" to listOf("العربية", "Cancel", "Annuler", "Cancelar", "Abbrechen", "Annuleren", "Avbryt", "Abbräche", "Annulla", "Annullar"),
        "رجوع" to listOf("العربية", "Back", "Retour", "Volver", "Zurück", "Terug", "Tillbaka", "Zrugg", "Indietro", "Enavos"),
        "← الرئيسية" to listOf("العربية", "← Home", "← Accueil", "← Inicio", "← Startseite", "← Home", "← Hem", "← Start", "← Home", "← Mussavia"),
        "＋ قناة" to listOf("العربية", "＋ Channel", "＋ Chaîne", "＋ Canal", "＋ Kanal", "＋ Kanaal", "＋ Kanal", "＋ Kanal", "＋ Canale", "＋ Chanagl"),
        "بحث فالقنوات…" to listOf("العربية", "Search channels…", "Rechercher des chaînes…", "Buscar canales…", "Kanäle suchen…", "Kanalen zoeken…", "Sök kanaler…", "Kanäl sueche…", "Cerca canali…", "Tschertgar chanals…"),
        "ترتيب / تصنيف" to listOf("العربية", "Organize / categorize", "Trier / catégoriser", "Ordenar / categorizar", "Sortieren / kategorisieren", "Ordenen / categoriseren", "Sortera / kategorisera", "Sortiere / kategorisiere", "Ordina / categorizza", "Ordinar / categorisar"),
        "حذف" to listOf("العربية", "Delete", "Supprimer", "Eliminar", "Löschen", "Verwijderen", "Ta bort", "Lösche", "Elimina", "Stizzar"),
        "تفاصيل القناة" to listOf("العربية", "Channel details", "Détails de la chaîne", "Detalles del canal", "Kanaldetails", "Kanaaldetails", "Kanaldetaljer", "Kanaldetail", "Dettagli canale", "Detagls dal chanagl"),
        "إيقاف" to listOf("العربية", "Stop", "Arrêter", "Detener", "Stoppen", "Stoppen", "Stoppa", "Stoppe", "Ferma", "Stoppar"),
        "معاينة" to listOf("العربية", "Preview", "Aperçu", "Vista previa", "Vorschau", "Voorbeeld", "Förhandsvisning", "Vorschau", "Anteprima", "Previsualisaziun"),
        "★ محفوظة" to listOf("العربية", "★ Saved", "★ Enregistrée", "★ Guardado", "★ Gespeichert", "★ Opgeslagen", "★ Sparad", "★ Gspeicheret", "★ Salvato", "★ Memorisà"),
        "تنظيم" to listOf("العربية", "Organize", "Organiser", "Organizar", "Organisieren", "Organiseren", "Organisera", "Organisiere", "Organizza", "Organisar"),
        "إظهار" to listOf("العربية", "Show", "Afficher", "Mostrar", "Anzeigen", "Tonen", "Visa", "Aazeige", "Mostra", "Mussar"),
        "إخفاء" to listOf("العربية", "Hide", "Masquer", "Ocultar", "Ausblenden", "Verbergen", "Dölj", "Verstecke", "Nascondi", "Z zuonder"),
        "إدارة القناة" to listOf("العربية", "Manage channel", "Gérer la chaîne", "Gestionar canal", "Kanal verwalten", "Kanaal beheren", "Hantera kanal", "Kanal verwalte", "Gestisci canale", "Administrar chanagl"),
        "＋ إضافة قناة" to listOf("العربية", "＋ Add channel", "＋ Ajouter une chaîne", "＋ Añadir canal", "＋ Kanal hinzufügen", "＋ Kanaal toevoegen", "＋ Lägg till kanal", "＋ Kanal hinzuefüege", "＋ Aggiungi canale", "＋ Agiuntar in chanagl"),
        "✎ تعديل القناة" to listOf("العربية", "✎ Edit channel", "✎ Modifier la chaîne", "✎ Editar canal", "✎ Kanal bearbeiten", "✎ Kanaal bewerken", "✎ Redigera kanal", "✎ Kanal bearbeite", "✎ Modifica canale", "✎ Modifitgar chanagl"),
        "⌫ حذف القناة" to listOf("العربية", "⌫ Delete channel", "⌫ Supprimer la chaîne", "⌫ Eliminar canal", "⌫ Kanal löschen", "⌫ Kanaal verwijderen", "⌫ Ta bort kanal", "⌫ Kanal lösche", "⌫ Elimina canale", "⌫ Stizzar chanagl"),
        "إغلاق" to listOf("العربية", "Close", "Fermer", "Cerrar", "Schließen", "Sluiten", "Stäng", "Schliesse", "Chiudi", "Serrar"),
        "شاهد" to listOf("العربية", "Watch", "Regarder", "Ver", "Ansehen", "Bekijken", "Titta", "Luege", "Guarda", "Guardar"),
        "قنوات الأطفال مباشرة" to listOf("العربية", "Kids live channels", "Chaînes jeunesse en direct", "Canales infantiles en directo", "Kinder-Live-Kanäle", "Live-kinderkanalen", "Barnkanaler live", "Chinderkanäl live", "Canali per bambini in diretta", "Chanals d'uffants live"),
        "الأفلام" to listOf("العربية", "Movies", "Films", "Películas", "Filme", "Films", "Filmer", "Film", "Film", "Films"),
        "المسلسلات" to listOf("العربية", "Series", "Séries", "Series", "Serien", "Series", "Serier", "Serien", "Serie", "Serias"),
        "اختار شنو بغيتي تتفرج اليوم ⭐" to listOf("العربية", "Choose what you want to watch today ⭐", "Choisissez ce que vous voulez regarder aujourd’hui ⭐", "Elige qué quieres ver hoy ⭐", "Wähle aus, was du heute sehen möchtest ⭐", "Kies wat je vandaag wilt kijken ⭐", "Välj vad du vill titta på idag ⭐", "Wähl uus, was du hüt luege wotsch ⭐", "Scegli cosa vuoi guardare oggi ⭐", "Tscherna quai che ti vuls guardar oz ⭐"),
        "قلب على فيلم أو كرتون أو مسلسل" to listOf("العربية", "Search for a movie, cartoon or series", "Rechercher un film, un dessin animé ou une série", "Buscar una película, dibujo animado o serie", "Film, Zeichentrick oder Serie suchen", "Zoek een film, tekenfilm of serie", "Sök efter film, tecknad film eller serie", "Suech en Film, Trickfilm oder Serie", "Cerca un film, cartone o serie", "Tschertga in film, in cartoon u ina seria"),
        "ما كايناش تريلرات متاحة دابا." to listOf("العربية", "No trailers are available right now.", "Aucune bande-annonce n’est disponible pour le moment.", "No hay tráileres disponibles ahora.", "Zurzeit sind keine Trailer verfügbar.", "Er zijn momenteel geen trailers beschikbaar.", "Inga trailers tillgängliga just nu.", "Im Momänt sind kei Trailer verfüegbar.", "Nessun trailer disponibile al momento.", "Actualmain na datti trailers disponibels."),
    )
    val index = when (language) { "ar" -> 0; "fr" -> 1; "en" -> 2; "es" -> 3; "de" -> 4; "nl" -> 5; "sv" -> 6; "gsw" -> 7; "it" -> 8; "rm" -> 9; else -> 2 }
    return translations[source]?.get(index) ?: source
}

private val Ink = Color(0xFF05070A)
private val Panel = Color(0xFF0C1016)
private val Raised = Color(0xFF111722)
private val Lime = Color(0xFFE8B83E)
private val GoldLight = Color(0xFFFFDA72)
private val TextMain = Color(0xFFF7F7F5)
private val TextMuted = Color(0xFF9DA5B1)
private val ErrorRed = Color(0xFFFF8177)
private const val CHANNEL_INFO_DURATION_MS = 5_000L
private const val CATALOG_PAGE_SIZE = 100
private const val PLAYER_API_BASE_URL = "https://admin.oatec.store/api/player"

private fun activateOatecCode(context: Context, code: String): PlayerSource {
    val prefs = context.getSharedPreferences("oatec_activation", Context.MODE_PRIVATE)
    val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("device_id", it).apply()
    }
    val payload = JSONObject()
        .put("code", code)
        .put("device_id", deviceId)
        .put("device_name", (Build.MANUFACTURER + " " + Build.MODEL).trim().take(255))
        .put("platform", "android")
        .put("app_version", "1.0.0")
    val connection = (URL(PLAYER_API_BASE_URL + "/activate-by-code").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 12000
        readTimeout = 15000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }
    return try {
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val response = runCatching { JSONObject(body) }.getOrNull()
        if (status !in 200..299 || response?.optBoolean("ok") != true) {
            val serverError = response?.optString("error").orEmpty()
            throw IOException(when {
                status == 401 -> "كود التفعيل غير صحيح أو سالات الصلاحية ديالو."
                status == 403 -> "الاشتراك ديالك ماشي فعّال أو سالات المدة ديالو."
                status == 409 -> "هاد الكود مربوط بجهاز آخر."
                serverError.isNotBlank() -> serverError
                else -> "تعذر التحقق من كود التفعيل (HTTP " + status + ")."
            })
        }
        val primary = response.optJSONObject("config")?.optJSONObject("primary")
            ?: throw IOException("السيرفر ما رجّعش إعدادات المصدر للحساب.")
        val sourceType = primary.optString("type").trim().lowercase()
        val server = primary.optString("server").trim().trimEnd('/')
        val username = primary.optString("username").trim()
        val password = primary.optString("password")
        val m3uUrl = primary.optString("url").trim()
        val xtreamUri = runCatching { Uri.parse(server) }.getOrNull()
        val validXtream = !xtreamUri?.host.isNullOrBlank() &&
            xtreamUri?.scheme in setOf("http", "https") &&
            username.isNotBlank() && password.isNotBlank()
        val m3uUri = runCatching { Uri.parse(m3uUrl) }.getOrNull()
        val validM3u = !m3uUri?.host.isNullOrBlank() && m3uUri?.scheme in setOf("http", "https")
        // Activation APIs can label an Xtream get.php playlist as M3U.
        // Convert only this recognizable endpoint; keep normal M3U URLs as M3U.
        val isXtreamGetPhp = m3uUri?.lastPathSegment.equals("get.php", ignoreCase = true)
        val urlUsername = m3uUri?.getQueryParameter("username").orEmpty().ifBlank { username }
        val urlPassword = m3uUri?.getQueryParameter("password").orEmpty().ifBlank { password }
        val validGetPhpXtream = isXtreamGetPhp && validM3u &&
            urlUsername.isNotBlank() && urlPassword.isNotBlank()
        val getPhpBase = if (isXtreamGetPhp) m3uUrl.substringBefore('?').substringBeforeLast('/') else ""
        when {
            sourceType == "xtream" && validXtream -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.XTREAM,
                serverUrl = server,
                username = username,
                password = password,
            )
            validGetPhpXtream -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.XTREAM,
                serverUrl = getPhpBase,
                username = urlUsername,
                password = urlPassword,
            )
            sourceType == "m3u" && validM3u -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.M3U,
                serverUrl = m3uUrl,
            )
            sourceType.isBlank() && validXtream -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.XTREAM,
                serverUrl = server,
                username = username,
                password = password,
            )
            sourceType.isBlank() && validM3u -> PlayerSource(
                id = UUID.randomUUID().toString(),
                name = "OATEC Subscription",
                type = SourceType.M3U,
                serverUrl = m3uUrl,
            )
            else -> throw IOException(
                "كود التفعيل صحيح، ولكن نوع المصدر أو بياناته فلوحة الأدمن غير متطابقة. تأكد أن الحساب مضبوط على M3U مع رابط صالح، أو Xtream مع Server وUsername وPassword."
            )
        }
    } finally { connection.disconnect() }
}
private enum class Section(val label: String) {
    HOME("Home"), LIVE("Live TV"), MOVIES("Movies"), SERIES("Series"), TRAILERS("Trailers"), DOWNLOADS("Downloads"), SOURCES("Sources"), SETTINGS("Settings"),
}

@Composable
fun PlayerTvApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val languagePreferences = remember { PlayerPreferences(context) }
    var appLanguage by remember { mutableStateOf(languagePreferences.appLanguage()) }
    var availableUpdate by remember { mutableStateOf<store.oatec.playertv.data.AppUpdate?>(null) }
    var dismissedVersion by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        availableUpdate = runCatching {
            store.oatec.playertv.data.AppUpdateRepository().checkForUpdate(context)
        }.getOrNull()
    }

    CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalLayoutDirection provides if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        PlayerTvAppContent(
            appLanguage = appLanguage,
            onLanguageChanged = { selected ->
                languagePreferences.setAppLanguage(selected)
                appLanguage = selected
            },
        )
    }

    val update = availableUpdate
    if (update != null && dismissedVersion != update.versionName) {
        AlertDialog(
            onDismissRequest = { dismissedVersion = update.versionName },
            title = { Text("تحديث جديد متوفر") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("OATEC PlayerTv ${update.versionName}")
                    if (update.releaseNotes.isNotBlank()) Text(update.releaseNotes)
                    Text(appText("غادي يتحل رابط تحميل APK. من بعد التحميل ثبّت التحديث يدوياً.", LocalAppLanguage.current))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissedVersion = update.versionName
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkDownloadUrl)))
                    }
                }) { Text("تحميل التحديث") }
            },
            dismissButton = {
                TextButton(onClick = { dismissedVersion = update.versionName }) { Text("لاحقاً") }
            },
        )
    }
}

@Composable
private fun PlayerTvAppContent(appLanguage: String, onLanguageChanged: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val compactLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp < 900
    val repository = remember { SourceRepository() }
    val mediaCatalogStore = remember { MediaCatalogStore(context) }
    val sourceStore = remember { SecureSourceStore(context) }
    val preferences = remember { PlayerPreferences(context) }
    val aboutRepository = remember { OatecAboutRepository() }
    val offlineDownloadStore = remember { OfflineDownloadStore(context) }
    var premiumCode by remember { mutableStateOf(runCatching { sourceStore.loadPremiumCode() }.getOrNull().orEmpty()) }
    var kidsModeActive by remember { mutableStateOf(preferences.kidsModeEnabled()) }
    // Use a separate lock per source/section so a slow LIVE import cannot block Movies.
    val catalogLoadMutexes = remember { mutableMapOf<String, Mutex>() }
    val sources = remember {
        mutableStateListOf<PlayerSource>().apply {
            addAll(runCatching { sourceStore.load() }.getOrDefault(emptyList()))
        }
    }
    var activeSourceId by rememberSaveable { mutableStateOf(sources.firstOrNull()?.id.orEmpty()) }
    val activeSource = sources.firstOrNull { it.id == activeSourceId }
    var catalog by remember { mutableStateOf(SourceCatalog()) }
        var loadedCatalogs by remember { mutableStateOf<Set<String>>(emptySet()) }
    // DB pagination offset must count raw rows, before Kids Mode filtering.
    var catalogPageOffsets by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // Open directly on the live channel list; users can switch straight to Movies, Series, or Settings.
    var section by remember { mutableStateOf(if (kidsModeActive) Section.HOME.name else Section.LIVE.name) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var liveCategoryNames by remember(activeSourceId) { mutableStateOf<List<String>>(emptyList()) }
    var movieCategories by remember(activeSourceId) { mutableStateOf<List<SourceCategory>>(emptyList()) }
    var seriesCategories by remember(activeSourceId) { mutableStateOf<List<SourceCategory>>(emptyList()) }
    var liveCategories by remember(activeSourceId) { mutableStateOf<List<SourceCategory>>(emptyList()) }
    var kidsChannels by remember(activeSourceId) { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var kidsMovies by remember(activeSourceId) { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var kidsSeries by remember(activeSourceId) { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var kidsYoutubeMovies by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var kidsYoutubeSeries by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var selectedMovieCategory by rememberSaveable { mutableStateOf("") }
    var selectedSeriesCategory by rememberSaveable { mutableStateOf("") }
    var loadedCategoryKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loadingCategoryKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var search by rememberSaveable { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<MediaEntry?>(null) }
    var selectedDetails by remember { mutableStateOf<MediaEntry?>(null) }
    var selectedTrailerYoutubeId by rememberSaveable { mutableStateOf("") }
    var trailerSearchBusy by remember { mutableStateOf(false) }
    var trailerSearchMessage by remember { mutableStateOf("") }
    var organizingChannel by remember { mutableStateOf<MediaEntry?>(null) }
    var pendingSeries by remember { mutableStateOf<MediaEntry?>(null) }
    var episodes by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var episodesBusy by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var activeLoadCount by remember { mutableStateOf(0) }
    var loadingCatalogKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var catalogProgress by remember { mutableStateOf(0f) }
    var catalogLoadedItems by remember { mutableStateOf(0) }
    var loadingMore by remember { mutableStateOf(false) }
    var preferenceRevision by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var showAddSource by remember { mutableStateOf(false) }
    var selectedKidsYoutubeId by rememberSaveable { mutableStateOf("") }
    var showNumberPad by remember { mutableStateOf(false) }
    var showKidsExitDialog by remember { mutableStateOf(false) }
    var kidsExitPinInput by remember { mutableStateOf("") }
    var kidsExitPinError by remember { mutableStateOf("") }
    var numberInput by rememberSaveable { mutableStateOf("") }
    var selectedChannelId by rememberSaveable { mutableStateOf("") }
    var epgPrograms by remember(activeSourceId) { mutableStateOf<List<EpgProgram>>(emptyList()) }
    var epgStatus by remember(activeSourceId) { mutableStateOf("") }
    var epgClock by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val currentSection = Section.entries.firstOrNull { it.name == section } ?: Section.LIVE
    val sectionHistory = remember { mutableStateListOf<Section>() }

    fun navigateTo(target: Section) {
        val current = Section.entries.firstOrNull { it.name == section } ?: Section.LIVE
        if (current != target) {
            sectionHistory.add(current)
            if (sectionHistory.size > 20) sectionHistory.removeAt(0)
        }
        section = target.name
        selectedCategory = "All"
        search = ""
        message = ""
        selectedMedia = null
        selectedDetails = null
    }

    fun goBack() {
        when {
            selectedDetails != null -> selectedDetails = null
            selectedMedia != null -> selectedMedia = null
            pendingSeries != null -> { pendingSeries = null; episodes = emptyList(); episodesBusy = false }
            sectionHistory.isNotEmpty() -> {
                val previous = sectionHistory.removeAt(sectionHistory.lastIndex)
                section = previous.name
                selectedCategory = "All"
                search = ""
                selectedMedia = null
                selectedDetails = null
            }
            currentSection != Section.LIVE -> section = Section.LIVE.name
            // Back from the root LIVE/channels screen must never close the app accidentally.
            // Return to Home instead; Android system Home/Recents remain available as usual.
            else -> {
                section = Section.HOME.name
                selectedCategory = "All"
                search = ""
                selectedMedia = null
                selectedDetails = null
            }
        }
    }

    // Keep Home navigation lightweight: clear transient detail/player state immediately,
    // rather than leaving episode lists and selection state attached to the previous screen.
    fun goHome() {
        section = Section.HOME.name
        selectedCategory = "All"
        search = ""
        selectedMedia = null
        selectedDetails = null
        pendingSeries = null
        episodes = emptyList()
        episodesBusy = false
        message = ""
    }

    // Keep children inside Kids Mode when Back is pressed, including from LIVE.
    // A normal app cannot block Android Home/Recents without device kiosk/lock-task setup.
    BackHandler(enabled = true) {
        if (kidsModeActive) {
            section = Section.HOME.name
            selectedCategory = "All"
            search = ""
            selectedMedia = null
            selectedDetails = null
        } else goBack()
    }

    fun loadCategory(source: PlayerSource, catalogSection: CatalogSection, category: SourceCategory, force: Boolean = false) {
        val sectionKey = "${source.id}:${catalogSection.name}"
        val categoryKey = "$sectionKey:${category.id}"
        if (!force && categoryKey in loadingCategoryKeys) return
        loadingCategoryKeys = loadingCategoryKeys + categoryKey
        scope.launch {
            activeLoadCount++
            loadingCatalogKeys = loadingCatalogKeys + sectionKey
            busy = true
            catalogProgress = 0f
            catalogLoadedItems = 0
            try {
                val cachedState = withContext(Dispatchers.IO) {
                    Triple(
                        mediaCatalogStore.isCategoryCached(source.id, catalogSection.name, category.id),
                        mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, category = category.name),
                        mediaCatalogStore.count(source.id, catalogSection.name, category = category.name),
                    )
                }
                val page = if (cachedState.first && !force) cachedState.second else {
                    withContext(Dispatchers.IO) {
                        mediaCatalogStore.beginCategoryImport(source.id, catalogSection.name, category).use { session ->
                            repository.loadCategoryBatched(source, catalogSection, category) { batch ->
                                session.insertBatch(batch)
                                val batchSize = batch.size
                                scope.launch {
                                    if (source.id == activeSourceId) {
                                        catalogLoadedItems += batchSize
                                        catalogProgress = (1f - kotlin.math.exp(-catalogLoadedItems / 9000f).toFloat()).coerceAtMost(0.92f)
                                    }
                                }
                            }
                            session.commit()
                        }
                        mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, category = category.name)
                    }
                }
                val total = withContext(Dispatchers.IO) { mediaCatalogStore.count(source.id, catalogSection.name, category = category.name) }
                if (source.id == activeSourceId) {
                    loadedCategoryKeys = loadedCategoryKeys + categoryKey
                    loadedCatalogs = loadedCatalogs + sectionKey
                    val isCurrentCategory = when (catalogSection) {
                        CatalogSection.LIVE -> selectedCategory == category.name
                        CatalogSection.MOVIES -> selectedMovieCategory == category.name
                        CatalogSection.SERIES -> selectedSeriesCategory == category.name
                    }
                    if (isCurrentCategory) {
                        catalogPageOffsets = catalogPageOffsets + (sectionKey to page.size)
                        when (catalogSection) {
                            CatalogSection.LIVE -> catalog = catalog.copy(liveChannels = applyKidsModeFilter(page, preferences))
                            CatalogSection.MOVIES -> catalog = catalog.copy(movies = applyKidsModeFilter(page, preferences))
                            CatalogSection.SERIES -> catalog = catalog.copy(series = applyKidsModeFilter(page, preferences))
                        }
                        catalogProgress = 1f
                        message = "$total ${catalogSection.name.lowercase()} items in ${category.name}."
                    }
                }
            } catch (error: Exception) {
                if (source.id == activeSourceId) message = error.message ?: "Could not load this category."
            } finally {
                loadingCategoryKeys = loadingCategoryKeys - categoryKey
                loadingCatalogKeys = loadingCatalogKeys - sectionKey
                activeLoadCount = (activeLoadCount - 1).coerceAtLeast(0)
                busy = activeLoadCount > 0
            }
        }
    }
    fun refresh(source: PlayerSource, requestedSection: Section = currentSection, force: Boolean = false) {
        val catalogSection = when (requestedSection) {
            Section.LIVE -> CatalogSection.LIVE
            Section.MOVIES -> CatalogSection.MOVIES
            Section.SERIES -> CatalogSection.SERIES
            else -> return
        }
        val loadKey = "${source.id}:${catalogSection.name}"
        if (!force && loadKey in loadedCatalogs) return
        message = ""
        if (source.type == SourceType.XTREAM) {
            // Import every provider category one at a time; each category is an
            // independent SQLite transaction, keeping memory bounded on large lists.
            scope.launch {
                activeLoadCount++
                loadingCatalogKeys = loadingCatalogKeys + loadKey
                busy = true
                catalogProgress = 0f
                catalogLoadedItems = 0
                try {
                    val categories = withContext(Dispatchers.IO) {
                        repository.loadCategories(source, catalogSection).also {
                            mediaCatalogStore.saveCategories(source.id, catalogSection.name, it)
                        }
                    }
                    if (source.id != activeSourceId) return@launch
                    when (catalogSection) {
                        CatalogSection.LIVE -> {
                            liveCategories = categories
                            liveCategoryNames = categories.map { it.name }
                            selectedCategory = "All"
                        }
                        CatalogSection.MOVIES -> {
                            movieCategories = categories
                            selectedMovieCategory = "All"
                        }
                        CatalogSection.SERIES -> {
                            seriesCategories = categories
                            selectedSeriesCategory = "All"
                        }
                    }
                    if (categories.isEmpty()) {
                        loadedCatalogs = loadedCatalogs + loadKey
                        message = "No categories were returned by this provider."
                        return@launch
                    }

                    val failures = mutableListOf<String>()
                    var completed = 0
                    for (category in categories) {
                        if (source.id != activeSourceId) break
                        val cached = withContext(Dispatchers.IO) {
                            mediaCatalogStore.isCategoryCached(source.id, catalogSection.name, category.id)
                        }
                        if (!cached || force) {
                            try {
                                withContext(Dispatchers.IO) {
                                    mediaCatalogStore.beginCategoryImport(source.id, catalogSection.name, category).use { session ->
                                        repository.loadCategoryBatched(source, catalogSection, category) { batch ->
                                            session.insertBatch(batch)
                                            val batchSize = batch.size
                                            scope.launch {
                                                if (source.id == activeSourceId) {
                                                    catalogLoadedItems += batchSize
                                                    catalogProgress = (1f - kotlin.math.exp(-catalogLoadedItems / 9000f).toFloat()).coerceAtMost(0.95f)
                                                }
                                            }
                                        }
                                        session.commit()
                                    }
                                }
                            } catch (error: Exception) {
                                failures += category.name
                            }
                        }
                        completed++
                        catalogProgress = completed.toFloat() / categories.size.toFloat()
                    }

                    if (source.id == activeSourceId) {
                        val result = withContext(Dispatchers.IO) {
                            mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0) to
                                mediaCatalogStore.count(source.id, catalogSection.name)
                        }
                        val firstPage = result.first
                        catalogPageOffsets = catalogPageOffsets + (loadKey to firstPage.size)
                        catalog = when (catalogSection) {
                            CatalogSection.LIVE -> catalog.copy(liveChannels = applyKidsModeFilter(firstPage, preferences))
                            CatalogSection.MOVIES -> catalog.copy(movies = applyKidsModeFilter(firstPage, preferences))
                            CatalogSection.SERIES -> catalog.copy(series = applyKidsModeFilter(firstPage, preferences))
                        }
                        if (failures.isEmpty() && completed == categories.size) {
                            loadedCatalogs = loadedCatalogs + loadKey
                            message = result.second.toString() + " " + catalogSection.name.lowercase() +
                                " items loaded from all " + categories.size + " categories."
                            catalogProgress = 1f
                        } else {
                            message = "Loaded available categories. Failed: " + failures.joinToString(", ") +
                                ". Tap Refresh to retry."
                        }
                    }
                } catch (error: Exception) {
                    if (source.id == activeSourceId) message = error.message ?: "Could not load provider categories."
                } finally {
                    loadingCatalogKeys = loadingCatalogKeys - loadKey
                    activeLoadCount = (activeLoadCount - 1).coerceAtLeast(0)
                    busy = activeLoadCount > 0
                }
            }
            return
        }
        message = ""
        catalogProgress = 0f
        catalogLoadedItems = 0
        activeLoadCount++
        loadingCatalogKeys = loadingCatalogKeys + loadKey
        busy = true
        scope.launch {
            try {
                // Show the last successfully saved catalogue immediately while a large
                // Xtream response is being downloaded. Read it in bounded DB pages.
                if (source.id == activeSourceId) {
                    val cachedState = withContext(Dispatchers.IO) {
                        mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0) to
                            mediaCatalogStore.count(source.id, catalogSection.name)
                    }
                    val cached = cachedState.first
                    val cachedCount = cachedState.second
                    catalogPageOffsets = catalogPageOffsets + (loadKey to cached.size)
                    if (cached.isNotEmpty() && source.id == activeSourceId) {
                        val safeCached = applyKidsModeFilter(cached, preferences)
                        catalog = when (catalogSection) {
                            CatalogSection.LIVE -> catalog.copy(liveChannels = safeCached)
                            CatalogSection.MOVIES -> catalog.copy(movies = safeCached)
                            CatalogSection.SERIES -> catalog.copy(series = safeCached)
                        }
                        message = "Loaded saved catalogue (" + cachedCount + " items)."
                    }
                    // A populated local catalogue is the default on app startup and
                    // section changes. Avoid downloading the full provider catalogue
                    // every launch; refresh remains available through the explicit
                    // refresh action (force=true).
                    if (cachedCount > 0L && !force && source.id == activeSourceId) {
                        loadedCatalogs = loadedCatalogs + loadKey
                        return@launch
                    }
                }
                catalogLoadMutexes.getOrPut(loadKey) { Mutex() }.withLock {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            // Stream bounded batches directly into one SQLite transaction.
                            // A failed response rolls back, preserving the last good catalogue.
                            val result = mediaCatalogStore.beginImport(source.id, catalogSection.name).use { session ->
                                var firstBatchDisplayed = false
                                val loaded = repository.loadCatalogBatched(source, catalogSection) { batch ->
                                    session.insertBatch(batch)
                                    val firstPage = batch.take(CATALOG_PAGE_SIZE)
                                    val batchSize = batch.size
                                    // Compose state changes are marshalled back to the main thread.
                                    scope.launch {
                                        if (source.id == activeSourceId) {
                                            if (!firstBatchDisplayed && firstPage.isNotEmpty()) {
                                                catalogPageOffsets = catalogPageOffsets + (loadKey to firstPage.size)
                                                val visiblePage = applyKidsModeFilter(firstPage, preferences)
                                                catalog = when (catalogSection) {
                                                    CatalogSection.LIVE -> catalog.copy(liveChannels = visiblePage)
                                                    CatalogSection.MOVIES -> catalog.copy(movies = visiblePage)
                                                    CatalogSection.SERIES -> catalog.copy(series = visiblePage)
                                                }
                                                firstBatchDisplayed = true
                                            }
                                            catalogLoadedItems += batchSize
                                            val count = catalogLoadedItems
                                            catalogProgress = (1f - kotlin.math.exp(-count / 9000f).toFloat()).coerceAtMost(0.92f)
                                        }
                                    }
                                }
                                session.commit()
                                catalogProgress = 1f
                                loaded
                            }
                            val firstPage = mediaCatalogStore.page(source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0)
                            val totalCount = mediaCatalogStore.count(source.id, catalogSection.name)
                            Triple(result, firstPage, totalCount)
                        }
                    }
                        .onSuccess { (result, firstPage, totalCount) ->
                            if (source.id == activeSourceId) {
                                catalogPageOffsets = catalogPageOffsets + (loadKey to firstPage.size)
                                catalog = when (catalogSection) {
                                    CatalogSection.LIVE -> catalog.copy(liveChannels = applyKidsModeFilter(firstPage, preferences))
                                    CatalogSection.MOVIES -> catalog.copy(movies = applyKidsModeFilter(firstPage, preferences))
                                    CatalogSection.SERIES -> catalog.copy(series = applyKidsModeFilter(firstPage, preferences))
                                }
                                if (catalogSection == CatalogSection.LIVE) selectedCategory = "All"
                                loadedCatalogs = loadedCatalogs + loadKey
                                message = "$totalCount ${catalogSection.name.lowercase()} items saved; loaded into the local catalogue and displayed without a 500-item cap."
                            }
                        }
                        .onFailure { if (source.id == activeSourceId) message = it.message ?: "Could not load this source." }
                    }
            } finally {
                loadingCatalogKeys = loadingCatalogKeys - loadKey
                activeLoadCount = (activeLoadCount - 1).coerceAtLeast(0)
                busy = activeLoadCount > 0
            }
        }
    }

    fun loadMore(requestedSection: Section) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return
        val catalogSection = when (requestedSection) {
            Section.LIVE -> CatalogSection.LIVE
            Section.MOVIES -> CatalogSection.MOVIES
            Section.SERIES -> CatalogSection.SERIES
            else -> return
        }
        if (loadingMore) return
        val loadKey = "${source.id}:${catalogSection.name}"
        val currentSize = catalogPageOffsets[loadKey] ?: when (catalogSection) {
            CatalogSection.LIVE -> catalog.liveChannels.size
            CatalogSection.MOVIES -> catalog.movies.size
            CatalogSection.SERIES -> catalog.series.size
        }
        val requestedSearch = search
        loadingMore = true
        scope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    // Use the database count and the current displayed page as the
                    // continuation offset. The query uses the same search predicate,
                    // so search results continue from the correct row rather than
                    // restarting or skipping items.
                    val categoryFilter = when (catalogSection) {
                        CatalogSection.LIVE -> selectedCategory.takeIf { it in liveCategoryNames }
                        CatalogSection.MOVIES -> selectedMovieCategory.takeIf { name -> movieCategories.any { it.name == name } }
                        CatalogSection.SERIES -> selectedSeriesCategory.takeIf { name -> seriesCategories.any { it.name == name } }
                    }
                    val total = mediaCatalogStore.count(
                        source.id, catalogSection.name, search = requestedSearch, category = categoryFilter,
                    )
                    if (currentSize.toLong() >= total) emptyList()
                    else mediaCatalogStore.page(
                        source.id, catalogSection.name,
                        limit = CATALOG_PAGE_SIZE, offset = currentSize,
                        search = requestedSearch, category = categoryFilter,
                    )
                }
                if (source.id == activeSourceId && requestedSearch == search && page.isNotEmpty()) {
                    catalogPageOffsets = catalogPageOffsets + (loadKey to (currentSize + page.size))
                    val combined = when (catalogSection) {
                        CatalogSection.LIVE -> catalog.liveChannels + page
                        CatalogSection.MOVIES -> catalog.movies + page
                        CatalogSection.SERIES -> catalog.series + page
                    }
                    val filteredCombined = applyKidsModeFilter(combined, preferences)
                    catalog = when (catalogSection) {
                        CatalogSection.LIVE -> catalog.copy(liveChannels = filteredCombined)
                        CatalogSection.MOVIES -> catalog.copy(movies = filteredCombined)
                        CatalogSection.SERIES -> catalog.copy(series = filteredCombined)
                    }
                }
            } catch (error: Exception) {
                if (source.id == activeSourceId) message = error.message ?: "Could not load the next catalogue page."
            } finally {
                loadingMore = false
            }
        }
    }

    LaunchedEffect(activeSourceId, activeSource?.epgUrl) {
        epgPrograms = emptyList()
        epgStatus = ""
        val guideUrl = activeSource?.epgUrl?.trim().orEmpty()
        if (guideUrl.isNotEmpty()) {
            runCatching { withContext(Dispatchers.IO) { EpgGuideClient().fetch(guideUrl) } }
                .onSuccess { programs -> epgPrograms = programs; epgStatus = if (programs.isEmpty()) "No programmes found in EPG guide." else "TV guide loaded: ${programs.size} programmes." }
                .onFailure { error -> epgStatus = "TV guide unavailable: ${error.message ?: "download failed"}" }
        }
    }

    LaunchedEffect(Unit) {
        while (true) { delay(60_000); epgClock = System.currentTimeMillis() }
    }

    LaunchedEffect(activeSourceId) {
        kidsChannels = emptyList()
        kidsMovies = emptyList()
        kidsSeries = emptyList()
        catalog = SourceCatalog()
        loadedCatalogs = emptySet()
        episodes = emptyList()
        episodesBusy = false
        pendingSeries = null
    }

    // Kids Mode downloads only provider categories identified as child/family
    // content. It does not import every adult category just to build the Kids view.
    LaunchedEffect(activeSourceId, kidsModeActive) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        if (!kidsModeActive) {
            kidsChannels = emptyList(); kidsMovies = emptyList(); kidsSeries = emptyList()
            return@LaunchedEffect
        }
        if (source.type != SourceType.XTREAM) {
            refresh(source, Section.LIVE)
            return@LaunchedEffect
        }
        runCatching {
            withContext(Dispatchers.IO) {
                suspend fun loadKidsSection(section: CatalogSection): Pair<List<SourceCategory>, List<MediaEntry>> {
                    val categories = repository.loadCategories(source, section)
                    mediaCatalogStore.saveCategories(source.id, section.name, categories)
                    val childCategories = categories.filter { isKidsCategoryName(it.name) }
                    for (category in childCategories) {
                        if (!mediaCatalogStore.isCategoryCached(source.id, section.name, category.id)) {
                            mediaCatalogStore.beginCategoryImport(source.id, section.name, category).use { session ->
                                repository.loadCategoryBatched(source, section, category) { batch ->
                                    session.insertBatch(batch)
                                }
                                session.commit()
                            }
                        }
                    }
                    val entries = childCategories.flatMap { category ->
                        mediaCatalogStore.page(
                            source.id, section.name, limit = 500, offset = 0, category = category.name,
                        )
                    }.distinctBy { it.id }
                    return categories to entries
                }
                Triple(
                    loadKidsSection(CatalogSection.LIVE),
                    loadKidsSection(CatalogSection.MOVIES),
                    loadKidsSection(CatalogSection.SERIES),
                )
            }
        }.onSuccess { loaded ->
            if (source.id == activeSourceId && kidsModeActive) {
                liveCategories = loaded.first.first
                movieCategories = loaded.second.first
                seriesCategories = loaded.third.first
                liveCategoryNames = loaded.first.first.map { it.name }.distinct().sorted()
                kidsChannels = applyKidsModeFilter(loaded.first.second, preferences)
                kidsMovies = applyKidsModeFilter(loaded.second.second, preferences)
                kidsSeries = applyKidsModeFilter(loaded.third.second, preferences)
            }
        }.onFailure {
            if (source.id == activeSourceId) message = "Kids categories could not be loaded: ${it.message ?: "provider error"}"
        }
    }

    // Load only the section the user is viewing. Each section reads its saved
    // SQLite catalogue first, so returning to it is fast; uncached sections are
    // imported in the background only when opened instead of downloading all
    // 36k+ items at startup.
    LaunchedEffect(activeSourceId, section, loadedCatalogs) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        val requestedSection = when (currentSection) {
            Section.LIVE -> Section.LIVE
            Section.MOVIES -> Section.MOVIES
            Section.SERIES -> Section.SERIES
            else -> return@LaunchedEffect
        }
        val loadKey = when (requestedSection) {
            Section.LIVE -> "${source.id}:${CatalogSection.LIVE.name}"
            Section.MOVIES -> "${source.id}:${CatalogSection.MOVIES.name}"
            Section.SERIES -> "${source.id}:${CatalogSection.SERIES.name}"
            else -> return@LaunchedEffect
        }
        if (loadKey !in loadedCatalogs) refresh(source, requestedSection)
    }

    LaunchedEffect(activeSourceId, section, search, loadedCatalogs, selectedCategory, preferenceRevision) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        val catalogSection = when (currentSection) {
            Section.LIVE -> CatalogSection.LIVE
            Section.MOVIES -> CatalogSection.MOVIES
            Section.SERIES -> CatalogSection.SERIES
            else -> return@LaunchedEffect
        }
        val loadKey = "${source.id}:${catalogSection.name}"
        if (loadKey !in loadedCatalogs) return@LaunchedEffect
        val requestedSearch = search
        val requestedCategory = selectedCategory
        val categoryFilter = when (catalogSection) {
            CatalogSection.LIVE -> requestedCategory.takeIf { it in liveCategoryNames }
            CatalogSection.MOVIES -> selectedMovieCategory.takeIf { name -> movieCategories.any { it.name == name } }
            CatalogSection.SERIES -> selectedSeriesCategory.takeIf { name -> seriesCategories.any { it.name == name } }
        }
        val filteredPage = withContext(Dispatchers.IO) {
            mediaCatalogStore.page(
                source.id, catalogSection.name, limit = CATALOG_PAGE_SIZE, offset = 0,
                search = requestedSearch, category = categoryFilter,
            )
        }
        if (source.id != activeSourceId || requestedSearch != search || currentSection.name != section ||
            selectedCategory != requestedCategory
        ) return@LaunchedEffect
        catalogPageOffsets = catalogPageOffsets + (loadKey to filteredPage.size)
        catalog = when (catalogSection) {
            CatalogSection.LIVE -> catalog.copy(liveChannels = applyKidsModeFilter(filteredPage, preferences))
            CatalogSection.MOVIES -> catalog.copy(movies = applyKidsModeFilter(filteredPage, preferences))
            CatalogSection.SERIES -> catalog.copy(series = applyKidsModeFilter(filteredPage, preferences))
        }
    }

    LaunchedEffect(activeSourceId, loadedCatalogs) {
        val source = sources.firstOrNull { it.id == activeSourceId } ?: return@LaunchedEffect
        if ("${source.id}:LIVE" !in loadedCatalogs) return@LaunchedEffect
        val categories = withContext(Dispatchers.IO) {
            val providerCategories = mediaCatalogStore.categoryList(source.id, CatalogSection.LIVE.name).map { it.name }
            if (providerCategories.isNotEmpty()) providerCategories else mediaCatalogStore.categories(source.id, CatalogSection.LIVE.name)
        }
        if (source.id == activeSourceId) {
            liveCategoryNames = categories.distinct().sortedBy { it.lowercase() }
        }
    }

    LaunchedEffect(kidsModeActive) {
        if (!kidsModeActive) {
            kidsYoutubeMovies = emptyList()
            kidsYoutubeSeries = emptyList()
            return@LaunchedEffect
        }
        kidsYoutubeMovies = emptyList()
        kidsYoutubeSeries = emptyList()
        val trendingBySection = withContext(Dispatchers.IO) {
            Pair(
                runCatching { searchYouTubeKidsTrending(Section.MOVIES) }.getOrDefault(emptyList()),
                runCatching { searchYouTubeKidsTrending(Section.SERIES) }.getOrDefault(emptyList()),
            )
        }
        kidsYoutubeMovies = trendingBySection.first
        kidsYoutubeSeries = trendingBySection.second
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { importLocalPlaylist(context, uri) }
                }.onSuccess { source ->
                    sources += source
                    sourceStore.save(sources)
                    activeSourceId = source.id
                    section = Section.HOME.name
                    message = "Playlist imported"
                }.onFailure { message = it.message ?: "Could not import playlist." }
                busy = false
            }
        }
    }

    val needsInitialSetup = sources.isEmpty()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            onPrimary = Ink,
            secondary = GoldLight,
            onSecondary = Ink,
            tertiary = Color(0xFF43D19E),
            background = Ink,
            onBackground = TextMain,
            surface = Panel,
            onSurface = TextMain,
            surfaceVariant = Raised,
            onSurfaceVariant = TextMuted,
            outline = Color(0xFF303743),
            error = ErrorRed,
            onError = Ink,
        ),
    ) {
        if (selectedKidsYoutubeId.isNotBlank()) {
            KidsYouTubePlayerDialog(videoId = selectedKidsYoutubeId, onDismiss = { selectedKidsYoutubeId = "" })
        }
        // Trailer previews appear in the selected title overlay, not in a separate page.
        if (needsInitialSetup) {
            InitialSetupScreen(
                savedPremiumCode = premiumCode,
                onComplete = { code, source ->
                    sourceStore.savePremiumCode(code)
                    premiumCode = code
                    sources.removeAll { it.name == "OATEC Subscription" }
                    sources += source
                    sourceStore.save(sources)
                    activeSourceId = source.id
                    section = Section.HOME.name
                },
            )
        } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x1AE8B83E), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.1f),
                            radius = size.maxDimension * 0.75f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(Color(0x12375B91), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height * 0.25f),
                            radius = size.maxDimension * 0.7f,
                        ),
                    )
                }
                .onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                    when (key.keyCode) {
                        AndroidKeyEvent.KEYCODE_MENU -> {
                            goHome()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_BACK -> { goBack(); true }
                        AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                            if (currentSection == Section.LIVE && catalog.liveChannels.isNotEmpty()) {
                                val channels = visibleChannels(catalog, selectedCategory, search, preferences)
                                val current = channels.indexOfFirst { it.id == selectedChannelId }
                                val index = if (current <= 0) channels.lastIndex else current - 1
                                channels.getOrNull(index)?.let { selectedChannelId = it.id; selectedMedia = it }
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            if (currentSection == Section.LIVE && catalog.liveChannels.isNotEmpty()) {
                                val channels = visibleChannels(catalog, selectedCategory, search, preferences)
                                val current = channels.indexOfFirst { it.id == selectedChannelId }
                                val index = if (current < 0 || current == channels.lastIndex) 0 else current + 1
                                channels.getOrNull(index)?.let { selectedChannelId = it.id; selectedMedia = it }
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_0,
                        AndroidKeyEvent.KEYCODE_1,
                        AndroidKeyEvent.KEYCODE_2,
                        AndroidKeyEvent.KEYCODE_3,
                        AndroidKeyEvent.KEYCODE_4,
                        AndroidKeyEvent.KEYCODE_5,
                        AndroidKeyEvent.KEYCODE_6,
                        AndroidKeyEvent.KEYCODE_7,
                        AndroidKeyEvent.KEYCODE_8,
                        AndroidKeyEvent.KEYCODE_9 -> {
                            numberInput = (numberInput + (key.keyCode - AndroidKeyEvent.KEYCODE_0)).takeLast(4)
                            true
                        }
                        in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> {
                            numberInput = (numberInput + (key.keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0)).takeLast(4)
                            true
                        }
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            numberInput.toIntOrNull()?.let { number ->
                                val match = numberedChannels(catalog.liveChannels, preferences)
                                    .firstOrNull { (channel, channelNumber) -> channelNumber == number }
                                match?.first?.let { selectedChannelId = it.id; selectedMedia = it }
                            }
                            numberInput = ""
                            true
                        }
                        else -> false
                    }
                },
        ) {
            if (kidsModeActive) {
                KidsDashboard(
                    compact = compactLandscape,
                    section = currentSection,
                    movies = (kidsMovies.ifEmpty { applyKidsModeFilter(catalog.movies, preferences) } + kidsYoutubeMovies).distinctBy { it.id },
                    series = (kidsSeries.ifEmpty { applyKidsModeFilter(catalog.series, preferences) } + kidsYoutubeSeries).distinctBy { it.id },
                    channels = kidsChannels.ifEmpty { applyKidsModeFilter(catalog.liveChannels, preferences) },
                    search = search,
                    onSearch = { search = it },
                    onSection = { next -> navigateTo(next); pendingSeries = null },
                    onPlay = { selectedMedia = it },
                    onPlayYoutube = { videoId -> selectedKidsYoutubeId = videoId },
                    onMovieDetails = { selectedDetails = it },
                    onExit = { kidsExitPinInput = ""; kidsExitPinError = ""; showKidsExitDialog = true },
                    onLoadMore = { loadMore(currentSection) },
                )
            } else if (currentSection == Section.HOME) {
                HomeDashboard(
                    compact = compactLandscape,
                    sourceName = activeSource?.name,
                    onSelect = { navigateTo(it) },
                    onExit = { (context as? Activity)?.finishAndRemoveTask() },
                )
            } else {
            Column(Modifier.fillMaxSize()) {
                // LIVE TV is a clean, full-screen channel list: no navigation strip,
                // title/search bar, or divider above the channels.
                if (currentSection !in setOf(Section.LIVE, Section.MOVIES, Section.SERIES, Section.SETTINGS)) {
                    NavigationRail(
                        selected = if (currentSection == Section.SOURCES) Section.SETTINGS else currentSection,
                        sourceName = activeSource?.name,
                        compact = compactLandscape,
                        onSelect = { navigateTo(it) },
                        onHome = { goBack() },
                        onExit = { (context as? Activity)?.finishAndRemoveTask() },
                    )
                }
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    if (currentSection !in setOf(Section.LIVE, Section.MOVIES, Section.SERIES, Section.SETTINGS)) {
                        TopBar(
                            title = currentSection.label,
                            sourceName = activeSource?.name,
                            search = search,
                            onSearch = { search = it },
                            busy = busy,
                            onRefresh = { activeSource?.let { refresh(it, currentSection, force = true) } },
                            showRefresh = currentSection in setOf(Section.LIVE, Section.MOVIES, Section.SERIES),
                            compact = compactLandscape,
                            onAddSource = { showAddSource = true },
                            onNumberPad = { showNumberPad = true },
                            numberInput = numberInput,
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    }
                    when (currentSection) {
                        Section.HOME -> Unit // HomeDashboard is rendered above this content switch.
                        Section.LIVE -> LiveScreen(
                            catalog = catalog,
                            epgPrograms = epgPrograms,
                            epgClock = epgClock,
                            epgStatus = epgStatus,
                            category = selectedCategory,
                            availableCategories = liveCategoryNames,
                            search = search,
                            preferences = preferences,
                            preferenceRevision = preferenceRevision,
                            onSearch = { search = it },
                            onHome = { goBack() },
                            message = message,
                            busy = busy,
                            catalogProgress = catalogProgress,
                            catalogLoadedItems = catalogLoadedItems,
                            onCategory = { name ->
                                if (selectedCategory != name) {
                                    selectedCategory = name
                                    catalog = catalog.copy(liveChannels = emptyList())
                                    val category = liveCategories.firstOrNull { it.name == name }
                                    if (category != null && activeSource != null) loadCategory(activeSource, CatalogSection.LIVE, category)
                                    else if (name in setOf("All", "Favorites", "Hidden") && activeSource != null) {
                                        scope.launch {
                                            val allCached = withContext(Dispatchers.IO) { mediaCatalogStore.page(activeSource.id, CatalogSection.LIVE.name, limit = CATALOG_PAGE_SIZE) }
                                            catalog = catalog.copy(liveChannels = applyKidsModeFilter(allCached, preferences))
                                        }
                                    }
                                }
                            },
                            onLoadMore = { loadMore(Section.LIVE) },
                            onPlay = {
                                selectedChannelId = it.id
                                selectedMedia = it.copy(title = preferences.channelTitle(it.id, it.title))
                            },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                            onOrganize = { organizingChannel = it },
                            onHide = { item -> preferences.setHidden(item.id, item.id !in preferences.hiddenChannels()); preferenceRevision++ },
                            onDelete = { item ->
                                if (item.id.startsWith("manual:")) preferences.removeCustomChannel(item.id)
                                else preferences.setHidden(item.id, true)
                                preferenceRevision++
                            },
                            onChanged = { preferenceRevision++ },
                        )
                        Section.MOVIES -> MediaScreen(
                            title = "Movies",
                            onHome = { goBack() },
                            onSearch = { search = it },
                            entries = catalog.movies,
                            providerCategories = movieCategories.map { it.name },
                            selectedProviderCategory = selectedMovieCategory,
                            onCategory = { name ->
                                if (name == "All" && activeSource != null) {
                                    selectedMovieCategory = "All"
                                    catalog = catalog.copy(movies = emptyList())
                                    scope.launch {
                                        val page = withContext(Dispatchers.IO) {
                                            mediaCatalogStore.page(activeSource.id, CatalogSection.MOVIES.name, limit = CATALOG_PAGE_SIZE)
                                        }
                                        if (activeSource.id == activeSourceId) {
                                            catalogPageOffsets = catalogPageOffsets + ("${activeSource.id}:MOVIES" to page.size)
                                            catalog = catalog.copy(movies = applyKidsModeFilter(page, preferences))
                                        }
                                    }
                                } else {
                                    val category = movieCategories.firstOrNull { it.name == name }
                                    if (category != null && activeSource != null) {
                                        selectedMovieCategory = name
                                        catalog = catalog.copy(movies = emptyList())
                                        loadCategory(activeSource, CatalogSection.MOVIES, category)
                                    }
                                }
                            },
                            search = search,
                            busy = "${activeSourceId}:MOVIES" in loadingCatalogKeys,
                            message = message,
                            catalogProgress = catalogProgress,
                            catalogLoadedItems = catalogLoadedItems,
                            preferences = preferences,
                            recentEntries = preferences.recentlyWatched(catalog.movies + episodes),
                            onLoadMore = { loadMore(Section.MOVIES) },
                            onPlay = { selectedDetails = it },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                        )
                        Section.SERIES -> SeriesScreen(
                            onHome = { goBack() },
                            onSearch = { search = it },
                            entries = catalog.series,
                            providerCategories = seriesCategories.map { it.name },
                            selectedProviderCategory = selectedSeriesCategory,
                            onCategory = { name ->
                                if (name == "All" && activeSource != null) {
                                    selectedSeriesCategory = "All"
                                    catalog = catalog.copy(series = emptyList())
                                    scope.launch {
                                        val page = withContext(Dispatchers.IO) {
                                            mediaCatalogStore.page(activeSource.id, CatalogSection.SERIES.name, limit = CATALOG_PAGE_SIZE)
                                        }
                                        if (activeSource.id == activeSourceId) {
                                            catalogPageOffsets = catalogPageOffsets + ("${activeSource.id}:SERIES" to page.size)
                                            catalog = catalog.copy(series = applyKidsModeFilter(page, preferences))
                                        }
                                    }
                                } else {
                                    val category = seriesCategories.firstOrNull { it.name == name }
                                    if (category != null && activeSource != null) {
                                        selectedSeriesCategory = name
                                        catalog = catalog.copy(series = emptyList())
                                        loadCategory(activeSource, CatalogSection.SERIES, category)
                                    }
                                }
                            },
                            episodes = episodes,
                            selectedSeries = pendingSeries,
                            search = search,
                            busy = episodesBusy,
                            message = message,
                            preferences = preferences,
                            onLoadMore = { loadMore(Section.SERIES) },
                            onSelect = { series ->
                                // Open the episode browser inside the Series screen, not in a dialog.
                                val source = activeSource
                                if (source == null || source.type != SourceType.XTREAM) {
                                    message = "Series episodes require an Xtream source."
                                } else {
                                    selectedDetails = null
                                    pendingSeries = series
                                    episodes = emptyList()
                                    episodesBusy = true
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                repository.loadSeriesEpisodes(source, series.id.substringAfterLast(':'))
                                            }
                                        }.onSuccess { episodes = it }
                                            .onFailure { message = it.message ?: "Could not load episodes." }
                                        episodesBusy = false
                                    }
                                }
                            },
                            onPlay = { selectedMedia = it },
                            onDownloadEpisode = { episode ->
                                runCatching { offlineDownloadStore.enqueue(episode) }
                                    .onSuccess { message = "بدأ تحميل الحلقة: ${episode.title}" }
                                    .onFailure { message = it.message ?: "تعذر تحميل الحلقة." }
                            },
                            onBackToSeries = {
                                pendingSeries = null
                                episodes = emptyList()
                                episodesBusy = false
                            },
                            onFavorite = { item -> preferences.setFavorite(item.id, item.id !in preferences.favorites()); preferenceRevision++ },
                        )
                        Section.TRAILERS -> KinoCheckTrailersScreen(onPlay = { videoId -> selectedKidsYoutubeId = videoId })
                        Section.DOWNLOADS -> OfflineDownloadsScreen(store = offlineDownloadStore, onHome = { goBack() }, onPlay = { selectedMedia = it }, onMessage = { message = it })
                        Section.SOURCES -> SourcesScreen(
                            sources = sources,
                            activeId = activeSourceId,
                            busy = busy,
                            onSelect = { source -> activeSourceId = source.id; navigateTo(Section.LIVE) },
                            onRefresh = { source -> refresh(source, Section.LIVE, force = true) },
                            onDelete = { source ->
                                sources.remove(source)
                                sourceStore.save(sources)
                                if (activeSourceId == source.id) activeSourceId = sources.firstOrNull()?.id.orEmpty()
                            },
                            onAdd = { showAddSource = true },
                            onImport = { filePicker.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "application/octet-stream")) },
                        )
                        Section.SETTINGS -> SettingsScreen(
                            onHome = { goBack() },
                            preferences = preferences,
                            selectedLanguage = appLanguage,
                            onLanguageChanged = onLanguageChanged,
                            onKidsModeChanged = { enabled ->
                                kidsModeActive = enabled
                                if (enabled) section = Section.HOME.name
                            },
                            aboutRepository = aboutRepository,
                            onManageSources = { navigateTo(Section.SOURCES) },
                            premiumCode = premiumCode,
                            onSavePremiumCode = { code ->
                                sourceStore.savePremiumCode(code)
                                premiumCode = code
                            },
                            onExit = { (context as? Activity)?.finishAndRemoveTask() },
                        )
                    }
                }
            }
            }
            if (showKidsExitDialog) {
                AlertDialog(
                    onDismissRequest = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" },
                    title = { Text(appText("Parent PIN required", LocalAppLanguage.current)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(appText("Enter the parent PIN to leave Kids Mode.", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                            OutlinedTextField(
                                value = kidsExitPinInput,
                                onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) kidsExitPinInput = value },
                                label = { Text(appText("Parent PIN", LocalAppLanguage.current)) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            )
                            if (kidsExitPinError.isNotBlank()) Text(kidsExitPinError, color = ErrorRed, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (preferences.verifyKidsPin(kidsExitPinInput)) {
                                preferences.setKidsModeEnabled(false)
                                kidsModeActive = false
                                section = Section.HOME.name
                                search = ""
                                kidsExitPinInput = ""
                                kidsExitPinError = ""
                                showKidsExitDialog = false
                            } else kidsExitPinError = "Incorrect PIN."
                        }) { Text(appText("Exit Kids Mode", LocalAppLanguage.current)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" }) { Text(appText("Cancel", LocalAppLanguage.current)) }
                    },
                )
            }
            if (showAddSource) {
                AddSourceDialog(
                    onDismiss = { showAddSource = false },
                    onSave = { source ->
                        sources.removeAll { it.id == source.id }
                        sources += source
                        sourceStore.save(sources)
                        activeSourceId = source.id
                        showAddSource = false
                    },
                )
            }
            if (showNumberPad) {
                NumberPadDialog(
                    channels = catalog.liveChannels,
                    preferences = preferences,
                    onDismiss = { showNumberPad = false },
                    onSelect = { channel ->
                        selectedChannelId = channel.id
                        selectedMedia = channel
                        showNumberPad = false
                    },
                )
            }
            organizingChannel?.let { channel ->
                OrganizeChannelDialog(
                    channel = channel,
                    channels = catalog.liveChannels,
                    preferences = preferences,
                    onDismiss = { organizingChannel = null },
                    onSave = { title, category, number, streamUrl, logoUrl ->
                        preferences.setChannelTitle(channel.id, title)
                        preferences.moveToCategory(channel.id, category)
                        preferences.moveChannelToNumber(channel.id, number, (catalog.liveChannels + preferences.customChannels()).map { it.id })
                        if (channel.id.startsWith("manual:")) preferences.saveCustomChannel(channel.copy(title = title, category = category, streamUrl = streamUrl, logoUrl = logoUrl))
                        preferenceRevision++
                        organizingChannel = null
                    },
                )
            }
            selectedMedia?.let { media ->
                PlaybackDialog(
                    entry = media,
                    channels = numberedChannels(catalog.liveChannels, preferences).map { (channel, _) ->
                        channel.copy(title = preferences.channelTitle(channel.id, channel.title))
                    },
                    preferences = preferences,
                    onDismiss = { selectedMedia = null },
                    onPlay = { selectedChannelId = it.id; selectedMedia = it },
                )
            }
            selectedDetails?.let { media ->
                LaunchedEffect(media.id, kidsModeActive) {
                    if (!kidsModeActive) {
                        selectedTrailerYoutubeId = ""
                        trailerSearchBusy = true
                        trailerSearchMessage = ""
                        runCatching { withContext(Dispatchers.IO) { searchYouTubeTrailer(media) } }
                            .onSuccess { videoId ->
                                if (videoId.isNullOrBlank()) trailerSearchMessage = "ما لقيناش تريلر مناسب لهاد العنوان."
                                else selectedTrailerYoutubeId = videoId
                            }
                            .onFailure { trailerSearchMessage = it.message ?: "تعذر البحث عن التريلر." }
                        trailerSearchBusy = false
                    }
                }
                MoviePreviewOverlay(
                    entry = media,
                    trailerVideoId = if (kidsModeActive) "" else selectedTrailerYoutubeId,
                    searching = trailerSearchBusy,
                    message = if (kidsModeActive) "" else trailerSearchMessage,
                    onDismiss = { selectedDetails = null; selectedTrailerYoutubeId = ""; trailerSearchMessage = "" },
                    onDownload = {
                        if (media.kind == MediaKind.MOVIE) {
                            runCatching { offlineDownloadStore.enqueue(media) }
                                .onSuccess { message = "بدأ تحميل: ${media.title}" }
                                .onFailure { message = it.message ?: "تعذر تحميل هذا المحتوى." }
                        } else {
                            message = "تحميل المسلسلات كيتطلب اختيار الحلقة أولاً."
                        }
                    },
                    onWatch = {
                        selectedDetails = null
                        selectedTrailerYoutubeId = ""
                        if (catalog.series.any { it.id == media.id }) {
                            val source = activeSource
                            if (source == null || source.type != SourceType.XTREAM) {
                                message = "Series episodes require an Xtream source."
                            } else {
                                pendingSeries = media
                                episodes = emptyList()
                                episodesBusy = true
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            repository.loadSeriesEpisodes(source, media.id.substringAfterLast(':'))
                                        }
                                    }.onSuccess { episodes = it }
                                        .onFailure { message = it.message ?: "Could not load episodes." }
                                    episodesBusy = false
                                }
                            }
                        } else {
                            selectedMedia = media
                        }
                    },
                )
            }
        }
        }
    }
}

@Composable
private fun InitialSetupScreen(
    savedPremiumCode: String,
    onComplete: (String, PlayerSource) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = LocalConfiguration.current
    val compactLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp < 900
    var entryScreen by rememberSaveable { mutableStateOf("welcome") }
    var premiumCode by rememberSaveable { mutableStateOf(savedPremiumCode) }
    var sourceType by rememberSaveable { mutableStateOf(SourceType.XTREAM.name) }
    var sourceName by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var activationBusy by rememberSaveable { mutableStateOf(false) }
    val activationScope = rememberCoroutineScope()
    val isXtream = sourceType == SourceType.XTREAM.name
    val accent = Color(0xFFE50920)
    val panel = Color(0xFF111111)
    val spacing = if (compactLandscape) 8.dp else 14.dp
    val horizontalPadding = if (compactLandscape) 20.dp else 28.dp

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF26070A), Color(0xFF090909), Color.Black),
                center = Offset(0.92f, 0.08f),
                radius = 1100f,
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (entryScreen == "welcome") {
            Column(
                Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = if (compactLandscape) 10.dp else 22.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(appText("TVI PLAYER", LocalAppLanguage.current), color = Color.White, fontSize = if (compactLandscape) 22.sp else 28.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(appText("ENTERTAINMENT, REFINED", LocalAppLanguage.current), color = accent, fontSize = if (compactLandscape) 9.sp else 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Surface(color = accent, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(if (compactLandscape) 42.dp else 56.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("T", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black) }
                    }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("شاشتك.\nتجربتك المميزة.", color = Color.White, fontSize = if (compactLandscape) 23.sp else 32.sp, fontWeight = FontWeight.Black, lineHeight = if (compactLandscape) 27.sp else 38.sp)
                    Text("وجهتك الأنيقة للتلفاز المباشر والأفلام والمسلسلات.", color = Color(0xFFBDBDBD), fontSize = if (compactLandscape) 10.sp else 14.sp)
                }
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = panel,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF393939)),
                        modifier = Modifier.weight(1f).fillMaxHeight(0.88f)
                            .clickable { entryScreen = "standard" },
                    ) {
                        Box(Modifier.fillMaxSize().padding(if (compactLandscape) 14.dp else 24.dp)) {
                            Text("◉", color = accent, fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd))
                            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                                Text(appText("STANDARD PLAYER", LocalAppLanguage.current), color = Color.White, fontSize = if (compactLandscape) 18.sp else 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text(appText("M3U  •  XTREAM CODES", LocalAppLanguage.current), color = Color(0xFFAAAAAA), fontSize = if (compactLandscape) 9.sp else 11.sp, letterSpacing = 1.sp)
                            }
                            Text("→", color = accent, fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart))
                        }
                    }
                    Surface(
                        color = Color(0xFF1B090B),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF512126)),
                        modifier = Modifier.weight(1.25f).fillMaxHeight(0.88f)
                            .clickable { entryScreen = "premium" },
                    ) {
                        Box(Modifier.fillMaxSize().padding(if (compactLandscape) 14.dp else 24.dp)) {
                            Text("✦", color = accent, fontSize = 34.sp, modifier = Modifier.align(Alignment.CenterStart))
                            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                                Surface(color = Color(0xFF310D12), shape = RoundedCornerShape(50.dp)) {
                                    Text(appText("PREMIUM", LocalAppLanguage.current), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(appText("OATEC PREMIUM CODE", LocalAppLanguage.current), color = Color.White, fontSize = if (compactLandscape) 17.sp else 25.sp, fontWeight = FontWeight.Black)
                                Text(appText("SECURE ACTIVATION", LocalAppLanguage.current), color = Color(0xFFAAAAAA), fontSize = 9.sp, letterSpacing = 1.4.sp)
                            }
                        }
                    }
                }
                Surface(
                    color = Color(0xFF090909),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/")))
                    },
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compactLandscape) 8.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("→", color = accent, fontSize = 20.sp)
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الإعدادات والمساعدة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("الملف الشخصي واللغة والتشغيل", color = Color(0xFFAAAAAA), fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("⚙", color = Color(0xFFBDBDBD), fontSize = 23.sp)
                    }
                }
            }
        } else if (entryScreen == "premium") {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(appText("OATEC PREMIUM CODE", LocalAppLanguage.current), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(appText("دخل كود التفعيل اللي توصّلتي به من OATEC باش نربطو الحساب ديالك أوتوماتيكياً.", LocalAppLanguage.current), color = Color(0xFFBDBDBD), fontSize = 13.sp)
                OutlinedTextField(
                    value = premiumCode,
                    onValueChange = { premiumCode = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("Premium code", LocalAppLanguage.current)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { entryScreen = "welcome"; error = "" }, modifier = Modifier.weight(1f), enabled = !activationBusy) { Text(appText("رجوع", LocalAppLanguage.current)) }
                    Button(
                        onClick = {
                            val normalizedCode = premiumCode.trim().uppercase()
                            if (normalizedCode.isBlank()) error = "دخل كود التفعيل ديالك."
                            else {
                                activationBusy = true
                                error = ""
                                activationScope.launch {
                                    try {
                                        val source = withContext(Dispatchers.IO) { activateOatecCode(context, normalizedCode) }
                                        premiumCode = normalizedCode
                                        onComplete(normalizedCode, source)
                                    } catch (e: Exception) {
                                        error = e.message ?: "تعذر التفعيل. تأكد من الكود والاتصال بالإنترنت."
                                    } finally { activationBusy = false }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !activationBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) {
                        if (activationBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("تفعيل وربط الحساب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = if (compactLandscape) 8.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 6.dp else 12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { entryScreen = "welcome"; error = "" }) { Text("← رجوع") }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(appText("STANDARD PLAYER", LocalAppLanguage.current), color = Color.White, fontSize = if (compactLandscape) 18.sp else 25.sp, fontWeight = FontWeight.Black)
                        Text("اختار طريقة الاتصال بمصدر القنوات", color = Color(0xFFBDBDBD), fontSize = if (compactLandscape) 9.sp else 12.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPill("Xtream Codes", isXtream) { sourceType = SourceType.XTREAM.name; error = "" }
                    CategoryPill("M3U URL", !isXtream) { sourceType = SourceType.M3U.name; error = "" }
                }
                if (compactLandscape) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SetupField(serverUrl, { serverUrl = it; error = "" }, if (isXtream) "Server URL" else "M3U playlist URL", Modifier.weight(1.6f), uri = true)
                        SetupField(sourceName, { sourceName = it }, "Source name (optional)", Modifier.weight(1f))
                    }
                    if (isXtream) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetupField(username, { username = it; error = "" }, "Username", Modifier.weight(1f))
                            SetupField(password, { password = it; error = "" }, "Password", Modifier.weight(1f), password = true)
                        }
                    }
                } else {
                    SetupField(sourceName, { sourceName = it }, "Source name (optional)", Modifier.fillMaxWidth())
                    SetupField(serverUrl, { serverUrl = it; error = "" }, if (isXtream) "Server URL" else "M3U playlist URL", Modifier.fillMaxWidth(), uri = true)
                    if (isXtream) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SetupField(username, { username = it; error = "" }, "Username", Modifier.weight(1f))
                            SetupField(password, { password = it; error = "" }, "Password", Modifier.weight(1f), password = true)
                        }
                    }
                }
                if (serverUrl.startsWith("http://", true)) {
                    Text(appText("HTTP غير مشفر؛ استعمل HTTPS إلا كان متاح.", LocalAppLanguage.current), color = ErrorRed, fontSize = 9.sp)
                }
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f, fill = !compactLandscape))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/"))) },
                        modifier = Modifier.weight(1f).height(if (compactLandscape) 42.dp else 48.dp),
                    ) { Text(appText("OATEC website", LocalAppLanguage.current), fontSize = 11.sp) }
                    Button(
                        onClick = {
                            val rawUrl = serverUrl.trim()
                            val parsed = Uri.parse(rawUrl)
                            val queryUser = parsed.getQueryParameter("username").orEmpty().trim()
                            val queryPassword = parsed.getQueryParameter("password").orEmpty()
                            val isXtreamPlaylistUrl =
                                parsed.lastPathSegment.equals("get.php", ignoreCase = true) &&
                                queryUser.isNotBlank() && queryPassword.isNotBlank()
                            val resolvedSource = if (isXtreamPlaylistUrl) {
                                val baseUrl = rawUrl.substringBefore('?').substringBeforeLast('/')
                                PlayerSource(
                                    id = UUID.randomUUID().toString(),
                                    name = sourceName.trim().ifBlank { "Xtream source" },
                                    type = SourceType.XTREAM,
                                    serverUrl = baseUrl,
                                    username = queryUser,
                                    password = queryPassword,
                                )
                            } else {
                                PlayerSource(
                                    id = UUID.randomUUID().toString(),
                                    name = sourceName.trim().ifBlank { if (isXtream) "Xtream source" else "M3U playlist" },
                                    type = if (isXtream) SourceType.XTREAM else SourceType.M3U,
                                    serverUrl = rawUrl,
                                    username = username.trim(),
                                    password = password,
                                )
                            }
                            when {
                                parsed.host.isNullOrBlank() || parsed.scheme !in setOf("http", "https") -> error = "دخل رابط HTTP أو HTTPS صحيح."
                                isXtream && (username.isBlank() || password.isBlank()) -> error = "دخل Username وPassword ديال Xtream."
                                isXtreamPlaylistUrl && (resolvedSource.serverUrl.isBlank() || resolvedSource.username.isBlank() || resolvedSource.password.isBlank()) -> error = "رابط Xtream M3U ناقص معلومات الدخول."
                                else -> onComplete(premiumCode.trim(), resolvedSource)
                            }
                        },
                        modifier = Modifier.weight(1f).height(if (compactLandscape) 42.dp else 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) { Text("اتصال وبدء الاستخدام", fontWeight = FontWeight.Bold, fontSize = if (compactLandscape) 11.sp else 14.sp) }
                }
            }
        }
    }
}

@Composable
private fun SetupField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    uri: Boolean = false,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 48.dp),
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(keyboardType = if (uri) KeyboardType.Uri else KeyboardType.Text),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
    )
}

@Composable
private fun SettingsScreen(
    onHome: () -> Unit,
    preferences: PlayerPreferences,
    selectedLanguage: String,
    onLanguageChanged: (String) -> Unit,
    onKidsModeChanged: (Boolean) -> Unit,
    aboutRepository: OatecAboutRepository,
    onManageSources: () -> Unit,
    premiumCode: String,
    onSavePremiumCode: (String) -> Unit,
    onExit: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var resumePlayback by remember { mutableStateOf(preferences.resumePlayback()) }
    var about by remember { mutableStateOf<store.oatec.playertv.data.OatecAbout?>(null) }
    var loadingAbout by remember { mutableStateOf(false) }
    var aboutError by remember { mutableStateOf("") }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var premiumCodeInput by remember(premiumCode) { mutableStateOf(premiumCode) }
    var premiumCodeSaved by remember { mutableStateOf(false) }
    var kidsEnabled by remember { mutableStateOf(preferences.kidsModeEnabled()) }
    var showKidsPinDialog by remember { mutableStateOf(false) }
    var kidsPinInput by remember { mutableStateOf("") }
    var kidsCurrentPinInput by remember { mutableStateOf("") }
    var showClearKidsPinDialog by remember { mutableStateOf(false) }
    var showKidsExitDialog by remember { mutableStateOf(false) }
    var kidsExitPinInput by remember { mutableStateOf("") }
    var kidsExitPinError by remember { mutableStateOf("") }
    var clearKidsPinInput by remember { mutableStateOf("") }
    var clearKidsPinError by remember { mutableStateOf("") }
    var kidsPinError by remember { mutableStateOf("") }
    var kidsPinSaved by remember { mutableStateOf(false) }
    var showKidsRulesDialog by remember { mutableStateOf(false) }
    var kidsRuleItemId by remember { mutableStateOf("") }
    var kidsRuleMessage by remember { mutableStateOf("") }

    fun loadAbout() {
        loadingAbout = true
        aboutError = ""
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { aboutRepository.fetch() } }
                .onSuccess { about = it }
                .onFailure { aboutError = it.message ?: "Could not load OATEC information." }
            loadingAbout = false
        }
    }

    LaunchedEffect(Unit) { loadAbout() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
            ) { Text(appText("← Back", selectedLanguage), fontSize = 12.sp) }
            Column(Modifier.weight(1f)) {
                Text(appText("SETTINGS", selectedLanguage), color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(appText("Playback, sources and OATEC preferences", selectedLanguage), color = TextMuted, fontSize = 12.sp)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(appText("APP LANGUAGE", selectedLanguage), color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(appText("Choose the language used by the app.", selectedLanguage), color = TextMuted, fontSize = 11.sp)
                val supportedLanguages = listOf(
                    "ar" to "العربية", "fr" to "Français", "en" to "English", "es" to "Español",
                    "de" to "Deutsch", "nl" to "Nederlands", "sv" to "Svenska",
                    "gsw" to "Schweizerdeutsch", "it" to "Italiano", "rm" to "Rumantsch",
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    supportedLanguages.forEach { (code, label) ->
                        if (selectedLanguage == code) {
                            Button(onClick = { onLanguageChanged(code) }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { onLanguageChanged(code) }) { Text(label) }
                        }
                    }
                }
                Text(appText("Language preference is saved on this device.", selectedLanguage), color = TextMuted, fontSize = 10.sp)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(appText("Sources & playlists", LocalAppLanguage.current), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(appText("Manage existing providers or import a playlist.", LocalAppLanguage.current), color = TextMuted, fontSize = 11.sp)
                }
                OutlinedButton(onClick = onManageSources) { Text(appText("Manage sources", LocalAppLanguage.current)) }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(appText("PLAYBACK", LocalAppLanguage.current), color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(appText("Resume VOD playback", LocalAppLanguage.current), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(appText("Continue movies and episodes from the saved position.", LocalAppLanguage.current), color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = resumePlayback,
                        onCheckedChange = {
                            resumePlayback = it
                            preferences.setResumePlayback(it)
                        },
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                var playbackEngine by remember { mutableStateOf(preferences.playbackEngine()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(appText("Playback engine", LocalAppLanguage.current), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(appText("Choose the playback engine for Live TV, movies and series.", LocalAppLanguage.current), color = TextMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("exoplayer" to "Media3 / ExoPlayer", "mpv" to "MPV").forEach { (value, label) ->
                            if (playbackEngine == value) {
                                Button(onClick = { playbackEngine = value; preferences.setPlaybackEngine(value) }) { Text(label) }
                            } else {
                                OutlinedButton(onClick = { playbackEngine = value; preferences.setPlaybackEngine(value) }) { Text(label) }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(appText("Watch history", LocalAppLanguage.current), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(appText("History is stored on this device.", LocalAppLanguage.current), color = TextMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { confirmClearHistory = true }) { Text(appText("Clear history", LocalAppLanguage.current)) }
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(appText("KIDS MODE", LocalAppLanguage.current), color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(appText("Parental PIN protection", LocalAppLanguage.current), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (preferences.hasKidsPin()) "PIN protected. Unknown content is hidden unless classified as child-safe or allowed manually."
                            else "Set a 4–8 digit PIN before enabling Kids Mode.",
                            color = TextMuted, fontSize = 11.sp,
                        )
                    }
                    Switch(checked = kidsEnabled, onCheckedChange = { enabled ->
                        if (enabled && !preferences.hasKidsPin()) {
                            kidsPinInput = ""
                            kidsCurrentPinInput = ""
                            kidsPinError = ""
                            showKidsPinDialog = true
                        } else if (!enabled && preferences.hasKidsPin()) {
                            // Exiting Kids Mode is parent-protected too.
                            kidsExitPinInput = ""
                            kidsExitPinError = ""
                            showKidsExitDialog = true
                        } else {
                            kidsEnabled = preferences.setKidsModeEnabled(enabled)
                            onKidsModeChanged(kidsEnabled)
                        }
                    })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showKidsRulesDialog = true; kidsRuleItemId = ""; kidsRuleMessage = "" }) { Text(appText("Manage content rules", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = {
                        kidsPinInput = ""
                        kidsCurrentPinInput = ""
                        kidsPinError = ""
                        kidsPinSaved = false
                        showKidsPinDialog = true
                    }) { Text(if (preferences.hasKidsPin()) "Set / change PIN" else "Create PIN") }
                    if (preferences.hasKidsPin()) OutlinedButton(onClick = { clearKidsPinInput = ""; clearKidsPinError = ""; showClearKidsPinDialog = true }) { Text(appText("Remove PIN", LocalAppLanguage.current)) }
                    if (kidsPinSaved) Text(appText("PIN saved", LocalAppLanguage.current), color = GoldLight, fontSize = 11.sp)
                }
                if (showClearKidsPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearKidsPinDialog = false; clearKidsPinInput = ""; clearKidsPinError = "" },
                        title = { Text(appText("Remove Kids Mode PIN?", LocalAppLanguage.current)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(appText("You must enter the current PIN. Removing it will also turn Kids Mode off.", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = clearKidsPinInput,
                                    onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) clearKidsPinInput = value },
                                    label = { Text(appText("Current PIN", LocalAppLanguage.current)) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )
                                if (clearKidsPinError.isNotBlank()) Text(clearKidsPinError, color = Color(0xFFFF7777), fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (preferences.clearKidsPin(clearKidsPinInput)) {
                                    kidsEnabled = false
                                    onKidsModeChanged(false)
                                    showClearKidsPinDialog = false
                                    clearKidsPinInput = ""
                                    clearKidsPinError = ""
                                } else clearKidsPinError = "Incorrect PIN."
                            }) { Text(appText("Remove PIN", LocalAppLanguage.current)) }
                        },
                        dismissButton = { TextButton(onClick = { showClearKidsPinDialog = false; clearKidsPinInput = ""; clearKidsPinError = "" }) { Text(appText("Cancel", LocalAppLanguage.current)) } },
                    )
                }
                if (showKidsRulesDialog) {
                    AlertDialog(
                        onDismissRequest = { showKidsRulesDialog = false },
                        title = { Text(appText("Kids Mode content rules", LocalAppLanguage.current)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(appText("Enter the exact catalogue item ID to allow or block it. These rules are saved on this device.", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                                OutlinedTextField(value = kidsRuleItemId, onValueChange = { kidsRuleItemId = it.trim() }, label = { Text(appText("Catalogue item ID", LocalAppLanguage.current)) }, singleLine = true)
                                Text("Allowed: ${preferences.kidsAllowedItemIds().size} · Blocked: ${preferences.kidsBlockedItemIds().size}", color = TextMuted, fontSize = 11.sp)
                                if (kidsRuleMessage.isNotBlank()) Text(kidsRuleMessage, color = GoldLight, fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    if (kidsRuleItemId.isBlank()) kidsRuleMessage = "Enter an item ID first."
                                    else { preferences.setKidsItemAllowed(kidsRuleItemId, true); kidsRuleMessage = "Item allowed on this device." }
                                }) { Text(appText("Allow", LocalAppLanguage.current)) }
                                TextButton(onClick = {
                                    if (kidsRuleItemId.isBlank()) kidsRuleMessage = "Enter an item ID first."
                                    else { preferences.setKidsItemAllowed(kidsRuleItemId, false); kidsRuleMessage = "Item blocked on this device." }
                                }) { Text(appText("Block", LocalAppLanguage.current)) }
                                TextButton(onClick = {
                                    if (kidsRuleItemId.isBlank()) kidsRuleMessage = "Enter an item ID first."
                                    else kidsRuleMessage = if (preferences.clearKidsItemRule(kidsRuleItemId)) "Override cleared; automatic classification applies." else "No override exists for this item."
                                }) { Text(appText("Clear rule", LocalAppLanguage.current)) }
                            }
                        },
                        dismissButton = { TextButton(onClick = { showKidsRulesDialog = false }) { Text(appText("Close", LocalAppLanguage.current)) } },
                    )
                }
                if (showKidsExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" },
                        title = { Text(appText("Parent PIN required", LocalAppLanguage.current)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(appText("Enter the parent PIN to leave Kids Mode.", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = kidsExitPinInput,
                                    onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) kidsExitPinInput = value },
                                    label = { Text(appText("Parent PIN", LocalAppLanguage.current)) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )
                                if (kidsExitPinError.isNotBlank()) Text(kidsExitPinError, color = ErrorRed, fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (preferences.verifyKidsPin(kidsExitPinInput)) {
                                    kidsEnabled = preferences.setKidsModeEnabled(false)
                                    showKidsExitDialog = false
                                    kidsExitPinInput = ""
                                    kidsExitPinError = ""
                                } else kidsExitPinError = "Incorrect PIN."
                            }) { Text(appText("Exit Kids Mode", LocalAppLanguage.current)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKidsExitDialog = false; kidsExitPinInput = ""; kidsExitPinError = "" }) { Text(appText("Cancel", LocalAppLanguage.current)) }
                        },
                    )
                }
                if (showKidsPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showKidsPinDialog = false; kidsPinInput = ""; kidsCurrentPinInput = ""; kidsPinError = "" },
                        title = { Text(if (preferences.hasKidsPin()) "Set a new Kids Mode PIN" else "Create Kids Mode PIN") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (preferences.hasKidsPin()) {
                                    OutlinedTextField(
                                        value = kidsCurrentPinInput,
                                        onValueChange = { value -> if (value.length <= 8 && value.all(Char::isDigit)) kidsCurrentPinInput = value },
                                        label = { Text(appText("Current PIN", LocalAppLanguage.current)) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    )
                                }
                                OutlinedTextField(
                                    value = kidsPinInput,
                                    onValueChange = { value ->
                                        if (value.length <= 8 && value.all(Char::isDigit)) kidsPinInput = value
                                        kidsPinError = ""
                                    },
                                    label = { Text(appText("4–8 digit PIN", LocalAppLanguage.current)) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                )
                                if (kidsPinError.isNotBlank()) Text(kidsPinError, color = Color(0xFFFF7777), fontSize = 12.sp)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val saved = if (preferences.hasKidsPin()) preferences.changeKidsPin(kidsCurrentPinInput, kidsPinInput) else preferences.setKidsPin(kidsPinInput)
                                if (saved) {
                                    kidsEnabled = true
                                    onKidsModeChanged(true)
                                    kidsPinSaved = true
                                    showKidsPinDialog = false
                                    kidsPinInput = ""
                                    kidsCurrentPinInput = ""
                                    kidsPinError = ""
                                } else kidsPinError = if (preferences.hasKidsPin()) "Check the current PIN and enter a new 4–8 digit PIN." else "Enter 4 to 8 digits."
                            }) { Text(appText("Save PIN", LocalAppLanguage.current)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showKidsPinDialog = false; kidsPinInput = ""; kidsCurrentPinInput = ""; kidsPinError = "" }) { Text(appText("Cancel", LocalAppLanguage.current)) }
                        },
                    )
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(appText("OATEC PREMIUM", LocalAppLanguage.current), color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                OutlinedTextField(
                    value = premiumCodeInput,
                    onValueChange = { premiumCodeInput = it; premiumCodeSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("Premium code (optional)", LocalAppLanguage.current)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Text(appText("The code is stored securely. Online verification is not available until OATEC provides an activation API.", LocalAppLanguage.current), color = TextMuted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onSavePremiumCode(premiumCodeInput.trim()); premiumCodeSaved = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) { Text(appText("Save code", LocalAppLanguage.current)) }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://oatec.store/"))) },
                    ) { Text(appText("Open OATEC website", LocalAppLanguage.current)) }
                    if (premiumCodeSaved) Text(appText(appText("Saved", LocalAppLanguage.current), LocalAppLanguage.current), color = GoldLight, fontSize = 11.sp)
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(appText("ABOUT OATEC", LocalAppLanguage.current), color = GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                when {
                    loadingAbout && about == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Lime, strokeWidth = 2.dp)
                        Text(appText("Loading information from oatec.store", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                    }
                    about != null -> {
                        Text(about!!.title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(about!!.description, color = TextMuted, fontSize = 13.sp)
                        Text(about!!.website, color = Lime, fontSize = 12.sp)
                    }
                    aboutError.isNotBlank() -> Text(aboutError, color = ErrorRed, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::loadAbout, enabled = !loadingAbout) { Text(appText("Refresh info", LocalAppLanguage.current)) }
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(about?.website ?: "https://oatec.store/")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) { Text(appText("Visit website", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = onExit) { Text(appText("Exit app", LocalAppLanguage.current)) }
                }
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text(appText("Clear watch history?", LocalAppLanguage.current)) },
            text = { Text(appText("Saved playback positions will also be removed from this device.", LocalAppLanguage.current)) },
            confirmButton = {
                TextButton(onClick = { preferences.clearWatchHistory(); confirmClearHistory = false }) { Text(appText("Clear", LocalAppLanguage.current)) }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text(appText("Cancel", LocalAppLanguage.current)) } },
        )
    }
}

@Composable
private fun HomeDashboard(
    compact: Boolean,
    sourceName: String?,
    onSelect: (Section) -> Unit,
    onExit: () -> Unit,
) {
    var selectedHomeCard by remember { mutableStateOf(Section.LIVE) }
    // Keep the home surface simple and cheap to render on Android TV devices.
    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 34.dp, vertical = if (compact) 12.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(if (compact) 62.dp else 104.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
            ) {
                Surface(
                    color = Color(0xFF090B0E),
                    shape = RoundedCornerShape(if (compact) 12.dp else 18.dp),
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(GoldLight, Lime, Color(0xFF8B5812)))),
                    modifier = Modifier.size(if (compact) 48.dp else 76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("▶", color = Lime, fontSize = if (compact) 16.sp else 25.sp, fontWeight = FontWeight.Black)
                            Text("TVI", color = GoldLight, fontSize = if (compact) 9.sp else 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("TVI", color = Lime, fontSize = if (compact) 20.sp else 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("PLAYER", color = TextMain, fontSize = if (compact) 20.sp else 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text(appText("Votre divertissement, simplement", LocalAppLanguage.current), color = TextMuted, fontSize = if (compact) 9.sp else 13.sp, maxLines = 1)
                    if (!sourceName.isNullOrBlank()) {
                        Text("●  ${sourceName}", color = Lime, fontSize = if (compact) 8.sp else 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(appText("Aucune source connectée", LocalAppLanguage.current), color = TextMuted, fontSize = if (compact) 8.sp else 10.sp, maxLines = 1)
                    }
                }
                Column(
                    Modifier.weight(1f).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛒", color = Lime, fontSize = if (compact) 17.sp else 27.sp)
                        Text("OATEC", color = GoldLight, fontSize = if (compact) 17.sp else 30.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(appText("STORE", LocalAppLanguage.current), color = TextMain.copy(alpha = 0.86f), fontSize = if (compact) 11.sp else 18.sp, fontWeight = FontWeight.Light, maxLines = 1)
                    }
                    Text(appText("VOTRE BOUTIQUE EN LIGNE", LocalAppLanguage.current), color = Lime.copy(alpha = 0.8f), fontSize = if (compact) 7.sp else 10.sp, letterSpacing = if (compact) 1.sp else 2.sp, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(appText("◉  WIFI", LocalAppLanguage.current), color = TextMain.copy(alpha = 0.8f), fontSize = if (compact) 9.sp else 13.sp)
                    OutlinedButton(
                        onClick = { onSelect(Section.SETTINGS) },
                        contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 13.dp, vertical = 2.dp),
                    ) { Text("⚙  ${if (compact) "Settings" else "SETTINGS"}", color = GoldLight, fontSize = if (compact) 9.sp else 12.sp) }
                }
            }

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cards = listOf(
                    HomeCardData(Section.LIVE, "TV LIVE", "Chaînes en direct", "▣", "LIVE TV", listOf(Color(0xFF193348), Color(0xFF07121C))),
                    HomeCardData(Section.MOVIES, "FILM", "Films à la demande", "▰", "CINÉMA", listOf(Color(0xFF4A351D), Color(0xFF100D0A))),
                    HomeCardData(Section.SERIES, "SÉRIES", "Séries & épisodes", "▤", "SÉRIES", listOf(Color(0xFF263746), Color(0xFF0A0D12))),
                    HomeCardData(Section.DOWNLOADS, "DOWNLOADS", "Regarder hors ligne", "⬇", "OFFLINE", listOf(Color(0xFF244536), Color(0xFF08140F))),
                    HomeCardData(Section.SETTINGS, "PARAMÈTRES", "Préférences & sources", "⚙", "OATEC", listOf(Color(0xFF3C2B13), Color(0xFF080A0D))),
                )
                cards.forEachIndexed { index, card ->
                    val active = card.section == selectedHomeCard
                    Surface(
                        color = Color(0xCC05080C),
                        shape = RoundedCornerShape(if (compact) 12.dp else 20.dp),
                        border = BorderStroke(
                            if (active) 2.dp else 1.dp,
                            if (active) Brush.linearGradient(listOf(GoldLight, Lime, Color(0xFFB87816)))
                            else Brush.linearGradient(listOf(Color(0xFF41434A), Color(0xFF20242B))),
                        ),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .onFocusChanged { if (it.isFocused) selectedHomeCard = card.section }
                            .focusable()
                            .clickable {
                                selectedHomeCard = card.section
                                onSelect(card.section)
                            }
                            .drawBehind {
                                if (active) {
                                    drawRect(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0x44E8B83E), Color.Transparent),
                                            center = Offset(size.width * 0.5f, size.height * 0.28f),
                                            radius = size.maxDimension * 0.65f,
                                        ),
                                    )
                                }
                            },
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = if (compact) 7.dp else 14.dp, vertical = if (compact) 8.dp else 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                Modifier.fillMaxWidth().weight(1f)
                                    .background(Brush.verticalGradient(card.artColors), RoundedCornerShape(if (compact) 8.dp else 14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    card.artLabel,
                                    color = TextMain.copy(alpha = 0.10f),
                                    fontSize = if (compact) 26.sp else 48.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(card.symbol, color = Lime, fontSize = if (compact) 37.sp else 62.sp, fontWeight = FontWeight.Bold)
                                    Box(Modifier.width(if (compact) 35.dp else 54.dp).height(3.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Lime, GoldLight, Color.Transparent))))
                                }
                            }
                            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
                            Text(card.title, color = if (active) GoldLight else TextMain, fontSize = if (compact) 12.sp else 23.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Text(card.subtitle, color = TextMuted, fontSize = if (compact) 8.sp else 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(Modifier.fillMaxWidth().padding(top = if (compact) 4.dp else 10.dp), horizontalArrangement = Arrangement.End) {
                                Text("›", color = if (active) Lime else TextMuted, fontSize = if (compact) 23.sp else 31.sp, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(if (compact) 28.dp else 42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(if (compact) 22.dp else 34.dp).height(2.dp).background(Lime))
                    Text("OATEC", color = Lime, fontSize = if (compact) 9.sp else 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Text(appText("STORE", LocalAppLanguage.current), color = TextMain, fontSize = if (compact) 9.sp else 13.sp, letterSpacing = 3.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(appText("♢  Service fiable", LocalAppLanguage.current), color = TextMuted, fontSize = if (compact) 8.sp else 11.sp, maxLines = 1)
                    Text(appText("ϟ  Accès rapide", LocalAppLanguage.current), color = TextMuted, fontSize = if (compact) 8.sp else 11.sp, maxLines = 1)
                    Text(appText("◉  Support 24/7", LocalAppLanguage.current), color = TextMuted, fontSize = if (compact) 8.sp else 11.sp, maxLines = 1)
                }
                Text(appText("Votre satisfaction est notre priorité", LocalAppLanguage.current), color = GoldLight, fontSize = if (compact) 8.sp else 13.sp, maxLines = 1)
            }
        }
    }
}

private data class HomeCardData(
    val section: Section,
    val title: String,
    val subtitle: String,
    val symbol: String,
    val artLabel: String,
    val artColors: List<Color>,
)

@Composable
private fun NavigationRail(
    selected: Section,
    sourceName: String?,
    compact: Boolean,
    onSelect: (Section) -> Unit,
    onHome: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF171D28), Color(0xFF0B0E14))))
            .padding(horizontal = if (compact) 8.dp else 22.dp, vertical = if (compact) 7.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = Color(0xFF0D1117),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Brush.linearGradient(listOf(Lime, GoldLight))),
                modifier = Modifier.size(if (compact) 32.dp else 40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("TVI", color = Lime, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(appText("TVI PLAYER", LocalAppLanguage.current), color = TextMain, fontSize = if (compact) 12.sp else 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (!sourceName.isNullOrBlank()) Text(sourceName, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(appText("← Back", LocalAppLanguage.current), fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 76.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(appText("Exit", LocalAppLanguage.current), fontSize = 10.sp)
            }
        }
        // Compact destinations replace the removed oversized top strip. Each opens content directly.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Section.LIVE to appText("Live TV", LocalAppLanguage.current),
                Section.MOVIES to appText("Movies", LocalAppLanguage.current),
                Section.SERIES to appText("Series", LocalAppLanguage.current),
                Section.TRAILERS to appText("Trailers", LocalAppLanguage.current),
                Section.SETTINGS to appText(appText("Settings", LocalAppLanguage.current), LocalAppLanguage.current),
                Section.SOURCES to appText("Sources", LocalAppLanguage.current),
            ).forEach { (destination, label) ->
                OutlinedButton(
                    onClick = { onSelect(destination) },
                    modifier = Modifier.heightIn(min = 44.dp),
                    contentPadding = PaddingValues(horizontal = if (compact) 9.dp else 13.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, if (selected == destination) Lime else Color.White.copy(alpha = 0.18f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selected == destination) Lime else TextMain,
                    ),
                ) {
                    Text(label, fontSize = if (compact) 10.sp else 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    sourceName: String?,
    search: String,
    onSearch: (String) -> Unit,
    busy: Boolean,
    onRefresh: () -> Unit,
    showRefresh: Boolean,
    compact: Boolean,
    onAddSource: () -> Unit,
    onNumberPad: () -> Unit,
    numberInput: String,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 28.dp, vertical = if (compact) 8.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = if (compact) 17.sp else 27.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(sourceName ?: "Add an IPTV source to begin", color = GoldLight.copy(alpha = 0.88f), fontSize = if (compact) 9.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (numberInput.isNotEmpty()) {
            Text("CH $numberInput", color = Lime, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (title == Section.LIVE.label) {
            OutlinedButton(onClick = onNumberPad, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text(appText("CH #", LocalAppLanguage.current), fontSize = 10.sp) }
        }
        SearchField(search, onSearch, Modifier.width(if (compact) 120.dp else 220.dp))
        if (showRefresh) {
            OutlinedButton(onClick = onRefresh, enabled = sourceName != null && !busy, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (compact) "Load" else "Refresh", fontSize = if (compact) 10.sp else 14.sp)
            }
        }
        Button(
            onClick = onAddSource,
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
            contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 16.dp, vertical = 4.dp),
        ) {
            Text(if (compact) "Add" else "Add source", fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 14.sp)
        }
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.height(54.dp),
        singleLine = true,
        placeholder = { Text(appText("Search", LocalAppLanguage.current), color = TextMuted, fontSize = 13.sp) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextMain),
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun LiveScreen(
    catalog: SourceCatalog,
    epgPrograms: List<EpgProgram>,
    epgClock: Long,
    epgStatus: String,
    category: String,
    availableCategories: List<String>,
    search: String,
    preferences: PlayerPreferences,
    preferenceRevision: Int,
    message: String,
    busy: Boolean,
    catalogProgress: Float,
    catalogLoadedItems: Int,
    onCategory: (String) -> Unit,
    onSearch: (String) -> Unit,
    onHome: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
    onOrganize: (MediaEntry) -> Unit,
    onHide: (MediaEntry) -> Unit,
    onDelete: (MediaEntry) -> Unit,
    onChanged: () -> Unit,
) {
    val allLiveChannels = remember(catalog.liveChannels, preferenceRevision) { catalog.liveChannels + preferences.customChannels() }
    val channels = remember(allLiveChannels, category, search, preferenceRevision) {
        visibleChannels(catalog.copy(liveChannels = allLiveChannels), category, search, preferences)
    }
    var selectedChannelId by rememberSaveable { mutableStateOf("") }
    val selectedChannel = channels.firstOrNull { it.id == selectedChannelId } ?: channels.firstOrNull()
    LaunchedEffect(channels) {
        if (selectedChannelId.isBlank() || channels.none { it.id == selectedChannelId }) {
            selectedChannelId = channels.firstOrNull()?.id.orEmpty()
        }
    }
    var previewRequested by remember { mutableStateOf(true) }
    var pendingDeleteChannel by remember { mutableStateOf<MediaEntry?>(null) }
    var channelActionsTarget by remember { mutableStateOf<MediaEntry?>(null) }
    var showAddChannelDialog by remember { mutableStateOf(false) }
    val categories = remember(availableCategories, allLiveChannels, preferenceRevision) {
        (listOf("All", "Favorites", "Hidden") + AUTO_CHANNEL_CATEGORIES + availableCategories +
            allLiveChannels.map { preferences.categoryFor(it.id, automaticChannelCategory(it)) } +
            preferences.customCategories()
        ).filter(String::isNotBlank).distinct()
    }
    val listState = rememberLazyGridState()
    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisible, channels.size) {
        if (channels.isNotEmpty() && lastVisible >= channels.size - 10) onLoadMore()
    }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION || screenWidthDp >= 800
    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = when { screenWidthDp >= 1400 -> 32.dp; screenWidthDp >= 700 -> 20.dp; else -> 8.dp }, vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 36.dp).widthIn(min = 72.dp).onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                    onHome()
                    true
                } else false
            }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp)) { Text(appText("← الرئيسية", LocalAppLanguage.current), fontSize = 10.sp) }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { showAddChannelDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text(appText("＋ قناة", LocalAppLanguage.current), fontSize = 11.sp) }
            OutlinedTextField(
                value = search, onValueChange = onSearch, modifier = Modifier.width(when { screenWidthDp >= 1200 -> 300.dp; screenWidthDp >= 700 -> 230.dp; else -> 145.dp }).heightIn(max = 54.dp),
                singleLine = true, placeholder = { Text(appText("بحث فالقنوات…", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextMain), shape = RoundedCornerShape(10.dp),
            )
        }
        LazyRow(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories, key = { it }) { item -> CategoryPill(item, category == item) { onCategory(item) } }
        }
        // Keep channel-manager actions reachable on phones as well as TV/desktop layouts.
        if (!isWideScreen && selectedChannel != null) {
            val detail = selectedChannel!!
            Row(
                Modifier.fillMaxWidth().padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "رقم ${channelNumber(detail, allLiveChannels, preferences)}",
                    color = Lime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onFavorite(detail) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(if (detail.id in preferences.favorites()) "★ المفضلة" else "☆ المفضلة", color = Lime, fontSize = 11.sp)
                }
                TextButton(onClick = { onOrganize(detail) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(appText("ترتيب / تصنيف", LocalAppLanguage.current), color = TextMain, fontSize = 11.sp)
                }
                TextButton(onClick = { pendingDeleteChannel = detail }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(appText("حذف", LocalAppLanguage.current), color = ErrorRed, fontSize = 11.sp)
                }
            }
        }
        when {
            busy && catalog.liveChannels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingState("Loading channels", catalogProgress, catalogLoadedItems) }
            channels.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("No channels available", message.ifBlank { "Add or refresh an IPTV source to load channels." }) }
            else -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = when { screenWidthDp >= 1500 -> 230.dp; screenWidthDp >= 1000 -> 190.dp; screenWidthDp >= 700 -> 155.dp; screenWidthDp <= 360 -> 118.dp; else -> 132.dp }),
                state = listState, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 3.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 10.dp else 6.dp),
                verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 10.dp else 6.dp),
            ) {
                gridItems(channels, key = { it.id }) { item ->
                    val selected = selectedChannel?.id == item.id
                    Surface(
                        color = if (selected) Raised else Panel, shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Lime else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth().height(if (isWideScreen) 104.dp else 78.dp).combinedClickable(
                            onClick = { selectedChannelId = item.id; previewRequested = true },
                            onLongClick = { channelActionsTarget = item },
                            onDoubleClick = { onPlay(item.copy(title = preferences.channelTitle(item.id, item.title))) },
                        ),
                    ) {
                        Row(Modifier.padding(if (isWideScreen) 10.dp else 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(if (isWideScreen) 52.dp else 38.dp).background(Color.Black, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                                if (item.logoUrl.isNotBlank()) AsyncImage(model = item.logoUrl, contentDescription = item.title, modifier = Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                                else Text(channelNumber(item, allLiveChannels, preferences).toString(), color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(channelNumber(item, catalog.liveChannels, preferences).toString().padStart(3, '0'), color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(preferences.channelTitle(item.id, item.title), color = TextMain, fontSize = if (isWideScreen) 13.sp else 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { onFavorite(item) }, modifier = Modifier.size(28.dp), contentPadding = PaddingValues(0.dp)) {
                                Text(if (item.id in preferences.favorites()) "★" else "☆", color = Lime, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
                if (isWideScreen && selectedChannel != null) {
                    val detail = selectedChannel!!
                    val current = EpgRepository.currentProgram(epgPrograms, detail.epgChannelId, epgClock)
                    val next = EpgRepository.nextProgram(epgPrograms, detail.epgChannelId, epgClock)
                    Surface(
                        modifier = Modifier.width((screenWidthDp * 0.30f).dp.coerceIn(300.dp, 440.dp)).fillMaxHeight(),
                        color = Panel,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    ) {
                        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(appText("تفاصيل القناة", LocalAppLanguage.current), color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(preferences.channelTitle(detail.id, detail.title), color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.fillMaxWidth().height(190.dp).background(Color.Black, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                ChannelMiniPreview(entry = detail, playing = previewRequested)
                            }
                            Text(current?.title?.let { "دابا · $it" } ?: "دابا · معلومات البرنامج غير متوفرة", color = GoldLight, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (next != null) Text("من بعد · ${next.title}", color = TextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onPlay(detail.copy(title = preferences.channelTitle(detail.id, detail.title))) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("▶ مشاهدة") }
                                OutlinedButton(onClick = { previewRequested = !previewRequested }, modifier = Modifier.weight(1f)) { Text(if (previewRequested) "إيقاف" else "معاينة") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(onClick = { onFavorite(detail) }) { Text(if (detail.id in preferences.favorites()) "★ محفوظة" else "☆ المفضلة", color = Lime) }
                                TextButton(onClick = { onOrganize(detail) }) { Text(appText("تنظيم", LocalAppLanguage.current), color = TextMuted) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(onClick = { onHide(detail) }) { Text(if (detail.id in preferences.hiddenChannels()) "إظهار" else "إخفاء", color = TextMuted) }
                                TextButton(onClick = { pendingDeleteChannel = detail }) { Text(appText("حذف", LocalAppLanguage.current), color = ErrorRed) }
                            }
                        }
                    }
                }
            }
        }
    }
    channelActionsTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { channelActionsTarget = null },
            title = { Text(appText("إدارة القناة", LocalAppLanguage.current)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(preferences.channelTitle(item.id, item.title), color = TextMain)
                    TextButton(onClick = { channelActionsTarget = null; showAddChannelDialog = true }) { Text(appText("＋ إضافة قناة", LocalAppLanguage.current)) }
                    TextButton(onClick = { channelActionsTarget = null; onOrganize(item) }) { Text(appText("✎ تعديل القناة", LocalAppLanguage.current)) }
                    TextButton(onClick = { channelActionsTarget = null; pendingDeleteChannel = item }) { Text(appText("⌫ حذف القناة", LocalAppLanguage.current), color = ErrorRed) }
                }
            },
            confirmButton = { TextButton(onClick = { channelActionsTarget = null }) { Text(appText("إغلاق", LocalAppLanguage.current)) } },
        )
    }
    if (showAddChannelDialog) {
        AddManualChannelDialog(
            onDismiss = { showAddChannelDialog = false },
            onSave = { title, url, logo, categoryName ->
                val item = MediaEntry(
                    id = "manual:" + UUID.randomUUID().toString(),
                    title = title,
                    category = categoryName,
                    streamUrl = url,
                    logoUrl = logo,
                )
                preferences.saveCustomChannel(item)
                preferences.addCategory(categoryName)
                selectedChannelId = item.id
                previewRequested = true
                showAddChannelDialog = false
                onChanged()
            },
        )
    }
    pendingDeleteChannel?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteChannel = null },
            title = { Text(appText("Delete channel?", LocalAppLanguage.current)) },
            text = { Text("Remove “${preferences.channelTitle(item.id, item.title)}” from the main channel list? You can restore it later from Hidden.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    pendingDeleteChannel = null
                }) { Text(appText("Delete", LocalAppLanguage.current), color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteChannel = null }) { Text(appText("Cancel", LocalAppLanguage.current)) }
            },
        )
    }
}
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun ChannelMiniPreview(entry: MediaEntry, playing: Boolean) {
    if (!playing) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (entry.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.logoUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(entry.title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(entry.id, entry.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)
            volume = 1f
            setMediaItem(mediaItemForStream(entry.streamUrl))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                keepScreenOn = true
            }
        },
        update = { it.player = player },
    )
}
@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Lime else Panel,
        contentColor = if (selected) Ink else TextMain,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.focusable().clickable(onClick = onClick),
    ) {
        Text(appText(label, LocalAppLanguage.current), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp))
    }
}

@Composable
private fun MediaScreen(
    title: String,
    entries: List<MediaEntry>,
    providerCategories: List<String>,
    selectedProviderCategory: String,
    onCategory: (String) -> Unit,
    search: String,
    busy: Boolean,
    message: String,
    catalogProgress: Float,
    catalogLoadedItems: Int,
    preferences: PlayerPreferences,
    recentEntries: List<MediaEntry>,
    onHome: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onFavorite: (MediaEntry) -> Unit,
) {
    var selectedShelf by rememberSaveable(title) { mutableStateOf(selectedProviderCategory.ifBlank { "All" }) }
    LaunchedEffect(selectedProviderCategory) { if (selectedProviderCategory.isNotBlank()) selectedShelf = selectedProviderCategory }
    val shelves = remember(entries, title, providerCategories) {
        if (providerCategories.isNotEmpty()) listOf("All") + providerCategories else {
        val providerCategories = entries.map { it.category.trim() }.filter { it.isNotBlank() && !it.equals("Other", true) }.distinct().sortedBy { it.lowercase() }
        val curated = listOf("Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western")
        val knownGenres = entries.flatMap { entry -> (entry.genre + "," + entry.title).split(",", "|", "/", "·").map { it.trim() } }
            .mapNotNull { raw -> curated.firstOrNull { it.equals(raw, true) } }.distinct()
        listOf("All") + (curated + providerCategories + knownGenres).distinct().filterNot { it.equals("All", true) }
        }
    }
    val searched = entries.filter { it.title.contains(search, true) || it.category.contains(search, true) || it.genre.contains(search, true) }
    val filtered = if (selectedShelf == "All") searched else if (selectedShelf in listOf("4K", "UHD", "1080p", "HD")) {
        searched.filter { qualityLabel(it).equals(selectedShelf, true) || (selectedShelf == "HD" && qualityLabel(it) == "1080p") }
    } else searched.filter { entry ->
        entry.category.equals(selectedShelf, true) ||
            entry.genre.split(",", "|", "/", "·").any { it.trim().equals(selectedShelf, true) } ||
            entry.title.contains(selectedShelf, true)
    }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTelevision = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val isWideScreen = isTelevision || screenWidthDp >= 900
    val horizontalInset = when {
        screenWidthDp >= 1600 -> 48.dp
        screenWidthDp >= 1000 -> 32.dp
        screenWidthDp >= 600 -> 24.dp
        else -> 14.dp
    }
    val posterWidth = when {
        isTelevision && screenWidthDp >= 1600 -> 220
        isTelevision -> 190
        screenWidthDp >= 1200 -> 184
        screenWidthDp >= 900 -> 166
        screenWidthDp >= 600 -> 148
        screenWidthDp <= 360 -> 112
        else -> 126
    }
    val headingFontSize = when {
        screenWidthDp >= 1200 -> 26.sp
        screenWidthDp >= 600 -> 23.sp
        else -> 20.sp
    }
    val listState = rememberLazyListState()
    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisible, entries.size, selectedShelf, search, title) {
        if (entries.isNotEmpty() && (selectedShelf == "All" || selectedShelf in providerCategories) && lastVisible >= listState.layoutInfo.totalItemsCount - 3) onLoadMore()
    }
    val isMovies = title.equals("Movies", true)
    val accent = if (isMovies) Color(0xFFE50914) else Color(0xFF8B5CF6)
    val rowShelves = remember(filtered, selectedShelf, search, title) {
        if (selectedShelf != "All" || search.isNotBlank()) listOf("Search results" to filtered)
        else {
            val grouped = linkedMapOf<String, List<MediaEntry>>()
            if (recentEntries.isNotEmpty()) grouped["Continue watching"] = recentEntries.take(12)
            val preferred = listOf("Action", "Adventure", "Animation", "Comedy", "Drama", "Family", "Fantasy", "Horror", "Sci-Fi", "Thriller")
            preferred.forEach { genre ->
                val matches = filtered.filter { entry ->
                    entry.category.equals(genre, true) || entry.genre.split(",", "|", "/", "·").any { it.trim().equals(genre, true) }
                }
                if (matches.isNotEmpty()) grouped[genre] = matches
            }
            filtered.groupBy { it.category.trim().ifBlank { "More to explore" } }
                .entries.sortedByDescending { it.value.size }.take(8).forEach { (name, items) ->
                    if (grouped.keys.none { it.equals(name, true) }) grouped[name] = items
                }
            if (grouped.isEmpty() || grouped.values.sumOf { it.size } < filtered.size / 2) grouped["Explore all titles"] = filtered
            grouped.entries.map { it.key to it.value }
        }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF09090B)).padding(horizontal = horizontalInset, vertical = if (isWideScreen) 8.dp else 3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.heightIn(min = 36.dp).widthIn(min = 72.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                        onHome()
                        true
                    } else false
                }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp)) { Text(appText("← الرئيسية", LocalAppLanguage.current), fontSize = 10.sp) }
            Spacer(Modifier.weight(1f))
            SearchField(search, onSearch, Modifier.width(when {
                screenWidthDp >= 1200 -> 280.dp
                screenWidthDp >= 800 -> 220.dp
                screenWidthDp >= 600 -> 185.dp
                else -> 132.dp
            }))
        }
        if (message.isNotBlank()) Text(message, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (shelves.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(shelves, key = { "shelf:$it" }) { shelf ->
                    CategoryPill(shelf, selectedShelf == shelf) { selectedShelf = shelf; if (shelf in providerCategories) onCategory(shelf) }
                }
            }
        }
        if (isMovies && search.isBlank() && selectedShelf == "All") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                listOf("4K", "UHD", "1080p", "HD").forEach { quality ->
                    val count = entries.count { qualityLabel(it).equals(quality, true) || (quality == "HD" && qualityLabel(it) == "1080p") }
                    if (count > 0) CategoryPill("$quality · $count", false) { selectedShelf = quality }
                }
            }
        }
        when {
            busy && entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingState("Loading library", catalogProgress, catalogLoadedItems) }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("Nothing to watch yet", message.ifBlank { "No titles match this category. Categories depend on the metadata supplied by your provider." }) }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                rowShelves.forEachIndexed { index, (rowTitle, rowEntries) ->
                    if (rowEntries.isNotEmpty()) {
                        item(key = "shelf-heading:$index:$rowTitle") {
                            Text(rowTitle, color = Color.White, fontSize = if (screenWidthDp >= 1200) 23.sp else if (isWideScreen) 20.sp else 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        item(key = "shelf-row:$index:$rowTitle") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 12.dp)) {
                                items(rowEntries.distinctBy { it.id }, key = { "poster:${rowTitle}:${it.id}" }) { entry ->
                                    PosterCard(entry = entry, favorite = entry.id in preferences.favorites(), onClick = { onPlay(entry) }, onFavorite = { onFavorite(entry) }, accent = accent, progressFraction = preferences.watchProgressFraction(entry.id), posterWidth = posterWidth, isWideScreen = isWideScreen)
                                }
                            }
                        }
                    }
                }
                if (busy) item(key = "catalog-loading") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { LoadingState("Loading more titles", catalogProgress, catalogLoadedItems) }
                }
            }
        }
    }
}

private fun mediaItemForStream(
    url: String,
    subtitles: List<MediaItem.SubtitleConfiguration> = emptyList(),
): MediaItem {
    // Some IPTV/Xtream providers use extensionless URLs or put query parameters after the
    // extension. Read the URI path only, then explicitly identify common adaptive formats.
    val path = runCatching { Uri.parse(url).path.orEmpty().lowercase() }.getOrDefault("")
    val mimeType = when {
        path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
        path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
        else -> null
    }
    return MediaItem.Builder()
        .setUri(url)
        .apply {
            if (mimeType != null) setMimeType(mimeType)
            if (subtitles.isNotEmpty()) setSubtitleConfigurations(subtitles)
        }
        .build()
}

private fun qualityLabel(entry: MediaEntry): String {
    val text = "${entry.title} ${entry.category} ${entry.streamUrl}".lowercase()
    return when {
        listOf("2160p", "4k", "uhd", "hevc 4k").any { it in text } -> "4K"
        listOf("1080p", "fhd", "full hd").any { it in text } -> "1080p"
        listOf("720p", "hd").any { it in text } -> "HD"
        else -> ""
    }
}

private fun streamQualityRank(entry: MediaEntry): Int = when (qualityLabel(entry)) {
    "4K" -> 3
    "1080p" -> 2
    "HD" -> 1
    else -> 0
}

private fun sameChannelIdentity(first: MediaEntry, second: MediaEntry): Boolean {
    fun identity(entry: MediaEntry): String = entry.title.lowercase()
        .replace(Regex("\\b(4k|uhd|2160p|1080p|720p|fhd|hd|hevc|h265|h\\.265)\\b"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    val a = identity(first)
    val b = identity(second)
    return a.isNotBlank() && a == b
}

@Composable
private fun PosterCard(
    entry: MediaEntry,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    accent: Color = Lime,
    progressFraction: Float = 0f,
    posterWidth: Int = 142,
    isWideScreen: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(posterWidth.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.68f).onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick),
        ) {
            Surface(
                color = Raised,
                shape = RoundedCornerShape(10.dp),
                border = if (focused) BorderStroke(2.dp, accent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (entry.logoUrl.isNotBlank()) AsyncImage(
                    model = entry.logoUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                ) else Box(contentAlignment = Alignment.Center) {
                    Text(entry.title.take(1).uppercase(), color = accent, fontSize = 35.sp, fontWeight = FontWeight.Black)
                }
            }
            val quality = qualityLabel(entry)
            if (quality.isNotBlank()) {
                Surface(color = accent, shape = RoundedCornerShape(bottomEnd = 8.dp), modifier = Modifier.align(Alignment.TopStart)) {
                    Text(quality, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            TextButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(if (favorite) "♥" else "+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (progressFraction > 0f) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp),
                    color = Color(0xFFE50914),
                    trackColor = Color.White.copy(alpha = 0.28f),
                )
            }
        }
        Text(entry.title, color = TextMain, fontSize = if (isWideScreen) 14.sp else 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        val subtitle = listOf(entry.year, entry.genre.ifBlank { entry.category }).firstOrNull { it.isNotBlank() }
        if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = if (isWideScreen) 11.sp else 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun OfflineDownloadsScreen(
    store: OfflineDownloadStore,
    onHome: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onMessage: (String) -> Unit,
) {
    var downloads by remember { mutableStateOf<List<OfflineItem>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            downloads = withContext(Dispatchers.IO) { store.list() }
            delay(800)
        }
    }
    Column(
        Modifier.fillMaxSize().background(Ink).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.heightIn(min = 44.dp).onPreviewKeyEvent { event ->
                    val key = event.nativeKeyEvent
                    if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                            AndroidKeyEvent.KEYCODE_ENTER,
                            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                            AndroidKeyEvent.KEYCODE_SPACE
                        )
                    ) {
                        onHome()
                        true
                    } else false
                }
            ) { Text("← Retour") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { (context as? Activity)?.finish() },
                modifier = Modifier.heightIn(min = 44.dp)
            ) { Text("Quitter") }
        }

        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun téléchargement", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 132.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(downloads, key = { it.downloadId }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Panel)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(12.dp))
                        ) {
                            if (item.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.logoUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(item.title.take(1).uppercase(), color = TextMain, fontSize = 36.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            if (!item.isComplete && item.progress > 0) {
                                Box(
                                    Modifier.align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .fillMaxHeight(item.progress / 100f)
                                        .background(Color(0xAAE50914))
                                )
                            }
                            Text(
                                if (item.isComplete) "100%" else "${item.progress}%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .padding(7.dp)
                                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                            if (item.isComplete) {
                                Box(Modifier.align(Alignment.TopEnd).padding(7.dp)
                                    .background(Lime, CircleShape).padding(5.dp)) {
                                    Text("✓", color = Ink, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text(
                            item.title,
                            color = TextMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        when {
                            item.isComplete -> {
                                Text("Téléchargement terminé", color = Color(0xFF70D6A2), fontSize = 10.sp)
                                OutlinedButton(
                                    onClick = { onPlay(item.asMediaEntry()) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp)
                                ) { Text("Regarder") }
                            }
                            item.status == android.app.DownloadManager.STATUS_PAUSED -> {
                                Text("Suspendu • ${item.progress}%", color = TextMuted, fontSize = 10.sp)
                                OutlinedButton(
                                    onClick = { store.resume(item.downloadId) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp)
                                ) { Text("Reprendre") }
                            }
                            item.status == android.app.DownloadManager.STATUS_FAILED -> {
                                Text("Échec du téléchargement", color = ErrorRed, fontSize = 10.sp)
                                OutlinedButton(
                                    onClick = { store.retry(item.downloadId) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp)
                                ) { Text("Réessayer") }
                            }
                            else -> {
                                Text("Téléchargement • ${item.progress}%", color = TextMuted, fontSize = 10.sp)
                                OutlinedButton(
                                    onClick = { store.pause(item.downloadId) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp)
                                ) { Text("Suspendre") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoviePreviewOverlay(
    entry: MediaEntry,
    trailerVideoId: String,
    searching: Boolean,
    message: String,
    onDismiss: () -> Unit,
    onWatch: () -> Unit,
    onDownload: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Transparent)) {
            if (trailerVideoId.isNotBlank()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                            overScrollMode = android.view.View.OVER_SCROLL_NEVER
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportZoom(false)
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><style>html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:transparent}iframe{position:fixed;inset:0;width:100%;height:100%;border:0;overflow:hidden}</style></head><body><iframe src="https://www.youtube-nocookie.com/embed/$trailerVideoId?autoplay=1&mute=1&controls=0&playsinline=1&rel=0&modestbranding=1&enablejsapi=1&origin=https%3A%2F%2Fwww.youtube-nocookie.com" title="Trailer preview" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></body></html>"""
                            loadDataWithBaseURL("https://www.youtube-nocookie.com/", html, "text/html", "UTF-8", null)
                        }
                    },
                    update = { view ->
                        // Recreate the player when the selected video changes; never keep the previous trailer.
                        if (view.tag != trailerVideoId) {
                            view.tag = trailerVideoId
                            val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"><style>html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:transparent}iframe{position:fixed;inset:0;width:100%;height:100%;border:0;overflow:hidden}</style></head><body><iframe src="https://www.youtube-nocookie.com/embed/$trailerVideoId?autoplay=1&mute=1&controls=0&playsinline=1&rel=0&modestbranding=1&enablejsapi=1&origin=https%3A%2F%2Fwww.youtube-nocookie.com" title="Trailer preview" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></body></html>"""
                            view.loadDataWithBaseURL("https://www.youtube-nocookie.com/", html, "text/html", "UTF-8", null)
                        }
                    },
                    onRelease = { it.stopLoading(); it.destroy() },
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent, Color(0xE609090B)))))
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF24201A), Color(0xFF09090B)))), contentAlignment = Alignment.Center) {
                    Text(if (searching) "Finding trailer…" else "Trailer preview unavailable", color = TextMuted)
                }
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(entry.title, color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val details = listOf(entry.year.ifBlank { entry.releaseDate.take(4) }, entry.genre, entry.rating).filter { it.isNotBlank() }.joinToString("  ·  ")
                if (details.isNotBlank()) Text(details, color = TextMuted, fontSize = 13.sp)
                if (message.isNotBlank()) Text(message, color = TextMuted, fontSize = 12.sp, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onWatch, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(appText("▶  Watch", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = onDownload) { Text(appText("⬇  Download", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = onDismiss) { Text(appText("Back", LocalAppLanguage.current)) }
                }
            }
        }
    }
}

@Composable
private fun MediaDetailsDialog(
    entry: MediaEntry,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onBrowseEpisodes: () -> Unit,
    trailerSearchBusy: Boolean = false,
    trailerSearchMessage: String = "",
    onFindTrailer: (MediaEntry) -> Unit = {},
    onDownload: () -> Unit,
) {
    val metadata = listOf(
        "Category" to entry.category,
        "Genre" to entry.genre,
        "Year" to entry.year.ifBlank { entry.releaseDate.take(4) },
        "Rating" to entry.rating,
        "Release date" to entry.releaseDate,
        "Director" to entry.director,
        "Cast" to entry.cast,
    ).filter { it.second.isNotBlank() }
    val isSeries = entry.kind == MediaKind.SERIES

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 960.dp).heightIn(max = 720.dp),
            color = Panel,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Lime.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        modifier = Modifier.weight(1f),
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = onDismiss) { Text(appText("Close", LocalAppLanguage.current), color = GoldLight) }
                }
                HorizontalDivider(color = TextMain.copy(alpha = 0.1f))
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (entry.logoUrl.isNotBlank()) {
                        AsyncImage(
                            model = entry.logoUrl,
                            contentDescription = entry.title,
                            modifier = Modifier.width(160.dp).heightIn(max = 250.dp),
                        )
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (metadata.isNotEmpty()) {
                            metadata.forEach { (label, value) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("$label:", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(value, color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                        Text(appText("Synopsis", LocalAppLanguage.current), color = Lime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            entry.description.ifBlank { "No synopsis or additional details are available from this source." },
                            color = TextMain,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                        if (isSeries) {
                            Text(appText("Open the episodes list to browse available seasons and episodes.", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                        }
                        if (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.SERIES) {
                            OutlinedButton(onClick = { onFindTrailer(entry) }, enabled = !trailerSearchBusy) {
                                Text(if (trailerSearchBusy) "Searching trailer…" else "▶ Find trailer")
                            }
                            if (trailerSearchMessage.isNotBlank()) Text(trailerSearchMessage, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                HorizontalDivider(color = TextMain.copy(alpha = 0.1f))
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                    if (!isSeries && entry.kind != MediaKind.LIVE) {
                        OutlinedButton(onClick = onDownload) { Text(appText("⬇ Download offline", LocalAppLanguage.current)) }
                    }
                    Button(
                        onClick = if (isSeries) onBrowseEpisodes else onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                    ) { Text(if (isSeries) "Episodes" else "Play") }
                }
            }
        }
    }
}

@Composable
private fun SeriesScreen(
    entries: List<MediaEntry>,
    providerCategories: List<String>,
    selectedProviderCategory: String,
    onCategory: (String) -> Unit,
    episodes: List<MediaEntry>,
    selectedSeries: MediaEntry?,
    search: String,
    busy: Boolean,
    message: String,
    preferences: PlayerPreferences,
    onHome: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onSelect: (MediaEntry) -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onDownloadEpisode: (MediaEntry) -> Unit,
    onBackToSeries: () -> Unit,
    onFavorite: (MediaEntry) -> Unit,
) {
    var selectedShelf by rememberSaveable { mutableStateOf(selectedProviderCategory.ifBlank { "All" }) }
    LaunchedEffect(selectedProviderCategory) { if (selectedProviderCategory.isNotBlank()) selectedShelf = selectedProviderCategory }
    val shelves = remember(entries, providerCategories) {
        if (providerCategories.isNotEmpty()) listOf("All") + providerCategories else {
            val discoveredCategories = entries.map { it.category.trim() }.filter { it.isNotBlank() && !it.equals("Other", true) }.distinct().sortedBy { it.lowercase() }
            val curated = listOf("Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Mystery", "Reality", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western")
            val genres = entries.flatMap { it.genre.split(",", "|", "/", "·").map(String::trim) }
                .mapNotNull { raw -> curated.firstOrNull { it.equals(raw, true) } }.distinct()
            listOf("All") + (curated + discoveredCategories + genres).distinct().filterNot { it.equals("All", true) }
        }
    }
    val searched = entries.filter { it.title.contains(search, true) || it.category.contains(search, true) || it.genre.contains(search, true) }
    val filtered = if (selectedShelf == "All") searched else searched.filter { entry ->
        entry.category.equals(selectedShelf, true) ||
            entry.genre.split(",", "|", "/", "·").any { it.trim().equals(selectedShelf, true) } ||
            entry.title.contains(selectedShelf, true)
    }
    val gridState = rememberLazyGridState()
    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisible, entries.size, selectedShelf, search, selectedSeries?.id) {
        if (selectedSeries == null && entries.isNotEmpty() && (selectedShelf == "All" || selectedShelf in providerCategories) &&
            lastVisible >= gridState.layoutInfo.totalItemsCount - 6
        ) onLoadMore()
    }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION || screenWidthDp >= 800
    val orderedEpisodes = remember(episodes) {
        episodes.sortedWith(
            compareBy<MediaEntry>(
                { Regex("\\d+").find(it.season)?.value?.toIntOrNull() ?: Int.MAX_VALUE },
                { Regex("\\d+").find(it.episodeNumber)?.value?.toIntOrNull() ?: Int.MAX_VALUE },
                { it.title.lowercase() },
            )
        )
    }
    val seasonGroups = remember(orderedEpisodes) { orderedEpisodes.groupBy { it.season.ifBlank { "Season 1" } } }

    Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = when { screenWidthDp >= 1400 -> 36.dp; screenWidthDp >= 700 -> 22.dp; else -> 10.dp }, vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = if (selectedSeries != null) onBackToSeries else onHome, modifier = Modifier.heightIn(min = 36.dp).widthIn(min = 72.dp).onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (key.action == AndroidKeyEvent.ACTION_DOWN && key.keyCode in setOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER, AndroidKeyEvent.KEYCODE_SPACE)) {
                    if (selectedSeries != null) onBackToSeries() else onHome()
                    true
                } else false
            }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp)) {
                Text(if (selectedSeries != null) appText("← Back to series", LocalAppLanguage.current) else appText("← الرئيسية", LocalAppLanguage.current), fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            if (selectedSeries == null) {
                SearchField(search, onSearch, Modifier.width(when { screenWidthDp >= 1200 -> 280.dp; screenWidthDp >= 700 -> 210.dp; else -> 135.dp }))
            }
        }
        if (message.isNotBlank()) Text(message, color = ErrorRed, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (selectedSeries == null && shelves.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                items(shelves, key = { "series-shelf:$it" }) { shelf ->
                    CategoryPill(shelf, selectedShelf == shelf) { selectedShelf = shelf }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        when {
            selectedSeries != null -> {
                val series = selectedSeries
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            color = Raised,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(if (isWideScreen) 132.dp else 104.dp).aspectRatio(1f),
                        ) {
                            if (series.logoUrl.isNotBlank()) {
                                AsyncImage(model = series.logoUrl, contentDescription = series.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else Box(contentAlignment = Alignment.Center) {
                                Text(series.title.take(1).uppercase(), color = Lime, fontSize = 30.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(series.title, color = TextMain, fontSize = if (isWideScreen) 22.sp else 18.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(listOf(series.year, series.genre, series.rating.takeIf { it.isNotBlank() }?.let { "★ $it" }.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "), color = GoldLight, fontSize = 11.sp, maxLines = 2)
                            if (series.description.isNotBlank()) Text(series.description, color = TextMuted, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text("${episodes.size} ${appText("Episodes", LocalAppLanguage.current)}", color = Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = TextMain.copy(alpha = 0.12f))
                    when {
                        busy -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { LoadingState("Loading episodes") }
                        orderedEpisodes.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            EmptyState("No episode data", "This provider did not return playable episodes.")
                        }
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = when { screenWidthDp >= 1500 -> 190.dp; screenWidthDp >= 1000 -> 160.dp; screenWidthDp >= 700 -> 140.dp; screenWidthDp <= 360 -> 112.dp; else -> 124.dp }),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(top = 3.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 12.dp else 8.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 12.dp else 8.dp),
                        ) {
                            gridItems(orderedEpisodes, key = { "episode:${it.id}" }) { episode ->
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Surface(
                                        color = Raised,
                                        shape = RoundedCornerShape(9.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    ) {
                                        AsyncImage(
                                            model = episode.logoUrl.ifBlank { series.logoUrl },
                                            contentDescription = episode.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                    Text(
                                        "${episode.episodeNumber.padStart(2, '0')}  ${episode.title}",
                                        color = TextMain,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(episode.season.ifBlank { "Season 1" }, color = TextMuted, fontSize = 9.sp, maxLines = 1)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = { onPlay(episode) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 3.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                                        ) { Text(appText("Play", LocalAppLanguage.current), fontSize = 9.sp) }
                                        OutlinedButton(
                                            onClick = { onDownloadEpisode(episode) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 3.dp),
                                        ) { Text(appText("⬇ Download offline", LocalAppLanguage.current), fontSize = 8.sp, maxLines = 1) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            filtered.isEmpty() && busy -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState("Loading series")
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("No series available", message.ifBlank { "Series metadata is available from supported Xtream sources." })
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = when { screenWidthDp >= 1500 -> 210.dp; screenWidthDp >= 1000 -> 180.dp; screenWidthDp >= 700 -> 150.dp; screenWidthDp <= 360 -> 108.dp; else -> 124.dp }),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 3.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 14.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 14.dp else 8.dp),
            ) {
                gridItems(filtered, key = { it.id }) { entry ->
                    val selected = entry.id == selectedSeries?.id
                    Column(Modifier.fillMaxWidth().clickable { onSelect(entry) }) {
                        Surface(
                            color = Raised,
                            border = if (selected) BorderStroke(2.dp, Lime) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        ) {
                            if (entry.logoUrl.isNotBlank()) AsyncImage(model = entry.logoUrl, contentDescription = entry.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Box(contentAlignment = Alignment.Center) { Text(entry.title.take(1).uppercase(), color = Lime, fontSize = 32.sp, fontWeight = FontWeight.Black) }
                        }
                        if (preferences.watchProgressFraction(entry.id) > 0f) {
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { preferences.watchProgressFraction(entry.id) },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = Color(0xFFE50914),
                                trackColor = Color.White.copy(alpha = 0.28f),
                            )
                        }
                        Text(entry.title, color = TextMain, fontSize = if (isWideScreen) 13.sp else 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                        TextButton(onClick = { onFavorite(entry) }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                            Text(if (entry.id in preferences.favorites()) "★ Saved" else "+ Favorite", color = Lime, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesScreen(
    sources: List<PlayerSource>,
    activeId: String,
    busy: Boolean,
    onSelect: (PlayerSource) -> Unit,
    onRefresh: (PlayerSource) -> Unit,
    onDelete: (PlayerSource) -> Unit,
    onAdd: () -> Unit,
    onImport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(appText("Your IPTV sources", LocalAppLanguage.current), color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(appText("Source access is provided by your IPTV provider.", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onImport) { Text(appText("Import M3U file", LocalAppLanguage.current)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text(appText(appText("Add source", LocalAppLanguage.current), LocalAppLanguage.current)) }
        }
        Spacer(Modifier.height(20.dp))
        if (sources.isEmpty()) EmptyState("No sources added", "Add an M3U playlist or an Xtream-compatible account.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sources, key = { it.id }) { source ->
                var focused by remember { mutableStateOf(false) }
                Surface(
                    color = if (source.id == activeId) Raised else Panel,
                    shape = RoundedCornerShape(9.dp),
                    border = if (focused || source.id == activeId) BorderStroke(1.dp, Lime.copy(alpha = 0.6f)) else null,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.focusable().clickable { onSelect(source) },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(source.name, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${source.type.name.replace('_', ' ')} | ${source.serverUrl}", color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (source.serverUrl.startsWith("http://", true)) {
                                Text(appText("Unencrypted HTTP source", LocalAppLanguage.current), color = ErrorRed, fontSize = 10.sp)
                            }
                        }
                        OutlinedButton(onClick = { onRefresh(source) }, enabled = !busy) { Text(appText(appText("Refresh", LocalAppLanguage.current), LocalAppLanguage.current)) }
                        TextButton(onClick = { onDelete(source) }) { Text(appText("Remove", LocalAppLanguage.current), color = ErrorRed) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSourceDialog(onDismiss: () -> Unit, onSave: (PlayerSource) -> Unit) {
    var type by rememberSaveable { mutableStateOf(SourceType.M3U.name) }
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var epgUrl by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appText("Add IPTV source", LocalAppLanguage.current)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SourceType.M3U, SourceType.XTREAM).forEach { option ->
                        CategoryPill(option.name, type == option.name) { type = option.name }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text(appText("Source name", LocalAppLanguage.current)) }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text(if (type == SourceType.M3U.name) "M3U URL" else "Server URL") }, singleLine = true)
                OutlinedTextField(epgUrl, { epgUrl = it }, label = { Text(appText("EPG XMLTV URL (optional)", LocalAppLanguage.current)) }, singleLine = true)
                if (type == SourceType.XTREAM.name || type == SourceType.M3U.name) {
                    OutlinedTextField(username, { username = it }, label = { Text(appText("Username (optional for M3U)", LocalAppLanguage.current)) }, singleLine = true)
                    OutlinedTextField(password, { password = it }, label = { Text(appText("Password (optional for M3U)", LocalAppLanguage.current)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    if (type == SourceType.M3U.name) {
                        Text(appText("If you enter a server address instead of a full playlist URL, the app can build the get.php M3U link using these credentials.", LocalAppLanguage.current), color = TextMuted, fontSize = 10.sp)
                    }
                }
                if (url.startsWith("http://", true)) Text(appText("HTTP is unencrypted. Prefer HTTPS when your provider supports it.", LocalAppLanguage.current), color = ErrorRed, fontSize = 11.sp)
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = Uri.parse(url.trim())
                if (name.isBlank()) error = "Enter a source name."
                else if (parsed.host.isNullOrBlank() || parsed.scheme !in setOf("http", "https")) error = "Enter a valid HTTP or HTTPS URL."
                else if (type == SourceType.XTREAM.name && (username.isBlank() || password.isBlank())) error = "Enter the provider username and password."
                else if (type == SourceType.M3U.name && (parsed.lastPathSegment.equals("get.php", true) || parsed.lastPathSegment.equals("player_api.php", true)) && (username.isBlank() || password.isBlank()) && parsed.getQueryParameter("username").isNullOrBlank()) error = "Enter the provider username and password for this playlist URL."
                else if (epgUrl.isNotBlank() && (Uri.parse(epgUrl.trim()).host.isNullOrBlank() || Uri.parse(epgUrl.trim()).scheme !in setOf("http", "https"))) error = "Enter a valid HTTP or HTTPS EPG URL."
                else {
                    val enteredUrl = url.trim()
                    val queryUser = parsed.getQueryParameter("username").orEmpty().ifBlank { username.trim() }
                    val queryPassword = parsed.getQueryParameter("password").orEmpty().ifBlank { password }
                    val finalUrl = if (type == SourceType.M3U.name &&
                        parsed.path.orEmpty().trim('/').isBlank() &&
                        queryUser.isNotBlank() && queryPassword.isNotBlank()
                    ) {
                        enteredUrl.trimEnd('/') + "/get.php?username=" +
                            Uri.encode(queryUser) + "&password=" + Uri.encode(queryPassword) +
                            "&type=m3u_plus&output=ts"
                    } else enteredUrl
                    onSave(
                        PlayerSource(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            type = SourceType.valueOf(type),
                            serverUrl = finalUrl,
                            username = username.trim(),
                            password = password,
                            epgUrl = epgUrl.trim(),
                        ),
                    )
                }
            }) { Text(appText("Save", LocalAppLanguage.current)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(appText("Cancel", LocalAppLanguage.current)) } },
    )
}

@Composable
private fun NumberPadDialog(
    channels: List<MediaEntry>,
    preferences: PlayerPreferences,
    onDismiss: () -> Unit,
    onSelect: (MediaEntry) -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    val channel = digits.toIntOrNull()?.let { number ->
        numberedChannels(channels, preferences).firstOrNull { it.second == number }?.first
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appText("Tune a channel", LocalAppLanguage.current)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(digits.ifBlank { "Enter a channel number" }, color = if (digits.isBlank()) TextMuted else Lime, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("Clear", "0", "Back")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            OutlinedButton(
                                onClick = {
                                    digits = when (key) {
                                        "Clear" -> ""
                                        "Back" -> digits.dropLast(1)
                                        else -> (digits + key).takeLast(4)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(key) }
                        }
                    }
                }
                Text(channel?.title ?: if (digits.isBlank()) "" else "No channel with that number", color = if (channel == null) TextMuted else TextMain, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { channel?.let(onSelect) }, enabled = channel != null) { Text(appText("Tune", LocalAppLanguage.current)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(appText("Cancel", LocalAppLanguage.current)) } },
    )
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun PlaybackDialog(
    entry: MediaEntry,
    channels: List<MediaEntry>,
    preferences: PlayerPreferences,
    onDismiss: () -> Unit,
    onPlay: (MediaEntry) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val trackSelector = remember(context) {
        DefaultTrackSelector(context).apply {
            // Adapt among provider HLS/DASH variants using measured bandwidth and supported device formats.
            parameters = buildUponParameters()
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setForceHighestSupportedBitrate(false)
                .build()
        }
    }
    val renderersFactory = remember(context) { DefaultRenderersFactory(context).setEnableDecoderFallback(true) }
    val player = remember(context) {
        ExoPlayer.Builder(context, renderersFactory).setTrackSelector(trackSelector).build().apply {
            val attrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
            setAudioAttributes(attrs, true)
            volume = 1f
        }
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var showList by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showMpvSettings by remember { mutableStateOf(false) }
    var mpvPaused by remember(entry.id) { mutableStateOf(false) }
    var activeEngine by remember(entry.id) { mutableStateOf(preferences.playbackEngine()) }
    var subtitleMessage by remember { mutableStateOf("") }
    var showOnlineSubtitleDialog by remember { mutableStateOf(false) }
    var onlineSubtitleQuery by remember(entry.id) { mutableStateOf(entry.title) }
    var onlineSubtitleLanguage by remember { mutableStateOf("ar") }
    var onlineSubtitleLoading by remember { mutableStateOf(false) }
    val playbackScope = rememberCoroutineScope()
    // Serialize MPV native preparation/playback with AndroidView release to avoid destroying a core mid-prepare.
    val mpvLifecycleMutex = remember(entry.id) { Mutex() }
    val externalAudioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && activeEngine == "exoplayer") {
            runCatching {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val factory = DefaultMediaSourceFactory(context)
                val videoSource = factory.createMediaSource(mediaItemForStream(entry.streamUrl))
                val audioSource = factory.createMediaSource(MediaItem.fromUri(uri))
                val position = player.currentPosition.coerceAtLeast(0L)
                player.setMediaSource(MergingMediaSource(videoSource, audioSource), position)
                player.prepare()
                player.playWhenReady = true
                subtitleMessage = "External audio added. If tracks do not align, choose a compatible audio file."
            }.onFailure { subtitleMessage = it.localizedMessage ?: "Could not combine this audio file with the stream." }
        } else if (uri != null) {
            subtitleMessage = "External audio is currently supported with ExoPlayer only."
        }
    }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && activeEngine == "exoplayer") {
            runCatching {
                runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                val path = uri.lastPathSegment.orEmpty().lowercase()
                val mime = when {
                    path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
                    path.endsWith(".ttml") || path.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
                    else -> MimeTypes.APPLICATION_SUBRIP
                }
                val subtitle = MediaItem.SubtitleConfiguration.Builder(uri).setMimeType(mime)
                    .setLanguage("und").setLabel("External subtitle").build()
                val mediaItem = mediaItemForStream(entry.streamUrl, listOf(subtitle))
                val position = player.currentPosition.coerceAtLeast(0L)
                player.setMediaItem(mediaItem, position)
                player.prepare()
                player.playWhenReady = true
                subtitleMessage = "External subtitle loaded. Enable it in the player's subtitle menu if needed."
            }.onFailure { subtitleMessage = it.localizedMessage ?: "Could not load this subtitle file." }
        } else if (uri != null) {
            subtitleMessage = "External subtitle file selection is currently supported with ExoPlayer."
        }
    }
    val qualityOptions = remember(entry.id, channels) {
        if (entry.kind != MediaKind.LIVE) emptyList()
        else channels.filter { it.kind == MediaKind.LIVE && sameChannelIdentity(it, entry) }
            .distinctBy { it.id }
            .sortedByDescending { streamQualityRank(it) }
    }
    val mpvCaBundle = remember(context) {
        File(context.filesDir, "mpv-cacert.pem").apply {
            if (!exists()) {
                context.assets.open("cacert.pem").use { input ->
                    outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
    var mpvAttempt by remember(entry.id) { mutableStateOf(0) }
    var controlsVisible by remember(entry.id) { mutableStateOf(true) }
    var mpvView by remember(entry.id, mpvAttempt) { mutableStateOf<MpvView?>(null) }
    val channelIndex = channels.indexOfFirst { it.id == entry.id }
    val latestEntry by rememberUpdatedState(entry)
    val latestPreferences by rememberUpdatedState(preferences)
    val latestEngine by rememberUpdatedState(activeEngine)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                loading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_ENDED && latestEntry.kind != MediaKind.LIVE && latestEngine == "exoplayer") {
                    latestPreferences.saveProgress(latestEntry.id, 0L, player.duration.coerceAtLeast(0L))
                }
            }
            override fun onPlayerError(playbackException: PlaybackException) {
                loading = false
                error = playbackException.localizedMessage ?: "ExoPlayer couldn't play this stream. Check the URL and stream format."
                // Separate 4K/HD playlist links cannot switch by bandwidth measurement alone,
                // but retry a clearly named lower-quality sibling after a live playback error.
                if (latestEntry.kind == MediaKind.LIVE) {
                    val currentRank = streamQualityRank(latestEntry)
                    val sibling = channels
                        .filter { it.id != latestEntry.id && it.kind == MediaKind.LIVE }
                        .filter { sameChannelIdentity(it, latestEntry) && streamQualityRank(it) in 1 until currentRank }
                        .maxByOrNull { streamQualityRank(it) }
                    if (sibling != null) {
                        onPlay(sibling)
                        return
                    }
                }
                if (latestEntry.kind != MediaKind.LIVE && latestEngine == "exoplayer" && latestPreferences.resumePlayback()) {
                    latestPreferences.saveProgress(latestEntry.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                }
                // Retry failed movie/series and live streams with MPV; it supports additional
                // containers, codecs, and IPTV stream formats that may fail in Media3.
                if (latestEngine == "exoplayer") {
                    error = "ExoPlayer could not play this stream. Trying MPV…"
                    activeEngine = "mpv"
                    return
                }
            }
        }
        player.addListener(listener)
        onDispose {
            if (latestEntry.kind != MediaKind.LIVE && latestPreferences.resumePlayback()) {
                if (latestEngine == "exoplayer") {
                    latestPreferences.saveProgress(latestEntry.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                } else if (latestEngine == "mpv") {
                    runCatching {
                        val positionMs = ((mpvView?.timePos ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        val durationMs = ((mpvView?.duration ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        if (positionMs > 0L) latestPreferences.saveProgress(latestEntry.id, positionMs, durationMs)
                    }
                }
            }
            player.stop()
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, entry.id, activeEngine) {
        while (true) {
            delay(5_000)
            if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                if (activeEngine == "exoplayer" && player.isPlaying) {
                    preferences.saveProgress(entry.id, player.currentPosition.coerceAtLeast(0L), player.duration.coerceAtLeast(0L))
                } else if (activeEngine == "mpv") {
                    runCatching {
                        val positionMs = ((mpvView?.timePos ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        val durationMs = ((mpvView?.duration ?: 0.0) * 1_000.0).toLong().coerceAtLeast(0L)
                        if (positionMs > 0L) preferences.saveProgress(entry.id, positionMs, durationMs)
                    }
                }
            }
        }
    }

    LaunchedEffect(entry.id, activeEngine, controlsVisible) {
        if (controlsVisible && entry.kind != MediaKind.LIVE) {
            delay(3_500)
            controlsVisible = false
        }
    }

    LaunchedEffect(entry.id, activeEngine, mpvAttempt, mpvView) {
        error = ""
        loading = true
        if (entry.kind != MediaKind.LIVE) preferences.recordWatched(entry.id)
        if (activeEngine == "exoplayer") {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItemForStream(entry.streamUrl))
            player.prepare()
            if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                val position = preferences.progress(entry.id)
                if (position > 5_000L) player.seekTo(position)
            }
            player.playWhenReady = true
        } else {
            // Do not leave Media3 decoding audio/video in the background when MPV is selected.
            player.stop()
            player.clearMediaItems()
            val view = mpvView ?: return@LaunchedEffect
            try {
                mpvLifecycleMutex.withLock {
                    currentCoroutineContext().ensureActive()
                    // MPV initialization is expensive on some TV boxes, so prepare its native core off the UI thread.
                    // Keep the lifecycle lock until preparation completes; AndroidView release waits on the same lock.
                    val prepared: PreparedCore = withContext(Dispatchers.IO) {
                        view.prepare(
                            MpvOptions(
                                tlsCaFile = mpvCaBundle,
                                // Prefer Arabic subtitles, then English/French when the stream provides them.
                                subtitleLanguages = listOf("ar", "ara", "en", "eng", "fr", "fre"),
                                // Automatically discover adjacent external subtitle files where supported.
                                // Prefer hardware decoding where available for high-resolution HEVC streams.
                                extra = mapOf(
                                    "sub-auto" to "all",
                                    // Keep hardware decoding as the first choice; retry in software only after failure.
                                    "hwdec" to if (mpvAttempt > 0) "no" else "auto-safe",
                                    "vd-lavc-software-fallback" to "yes",
                                ),
                            ),
                        )
                    }
                    currentCoroutineContext().ensureActive()
                    view.attach(prepared)
                    currentCoroutineContext().ensureActive()
                    view.playFile(entry.streamUrl).getOrThrow()
                    currentCoroutineContext().ensureActive()
                    if (entry.kind != MediaKind.LIVE && preferences.resumePlayback()) {
                        val resumePosition = preferences.progress(entry.id)
                        if (resumePosition > 5_000L) {
                            // MPV reports positions in seconds; the shared progress store uses milliseconds.
                            view.timePos = resumePosition / 1_000.0
                        }
                    }
                }
                loading = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (mpvAttempt == 0) {
                    // Some TV boxes fail with their hardware decoder on specific codecs/streams.
                    // Recreate MPV once with software decoding before showing a terminal error.
                    error = "MPV is retrying with software decoding…"
                    mpvAttempt += 1
                } else {
                    loading = false
                    error = failure.localizedMessage ?: "MPV could not start this stream. Check the source URL or device codec support."
                }
            }
        }
    }

    DisposableEffect(context, entry.kind) {
        val activity = generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
        val decorView = activity?.window?.decorView
        val previousUiVisibility = decorView?.systemUiVisibility ?: 0
        if (entry.kind == MediaKind.LIVE && decorView != null) {
            decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
        onDispose {
            if (decorView != null) decorView.systemUiVisibility = previousUiVisibility
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().background(Ink).onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (key.action != AndroidKeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (key.keyCode) {
                    AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.KEYCODE_ESCAPE -> {
                        if (showList) { showList = false; true } else false
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        if (channels.isEmpty()) false else {
                            onPlay(channels[if (channelIndex <= 0) channels.lastIndex else channelIndex - 1]); true
                        }
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (channels.isEmpty()) false else {
                            onPlay(channels[if (channelIndex < 0 || channelIndex == channels.lastIndex) 0 else channelIndex + 1]); true
                        }
                    }
                    else -> false
                }
            },
        ) {
            if (activeEngine == "mpv") {
                key(mpvAttempt) {
                    AndroidView(
                        factory = { viewContext ->
                            MpvView(viewContext).apply {
                                keepScreenOn = true
                                setOnClickListener { controlsVisible = true }
                            }.also { mpvView = it }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { releasedView ->
                            // This cleanup must outlive the composition: rememberCoroutineScope is cancelled
                            // as the dialog leaves composition and could otherwise skip native destroy().
                            // Serialize release with prepare()/playFile() so the native core is never torn down mid-call.
                            kotlinx.coroutines.CoroutineScope(
                                kotlinx.coroutines.SupervisorJob() + Dispatchers.IO,
                            ).launch {
                                mpvLifecycleMutex.withLock { releasedView.destroy() }
                            }
                        },
                    )
                }
            } else {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            // Native ExoPlayer controls auto-hide; custom overlays use the same timeout.
                            useController = entry.kind != MediaKind.LIVE
                            controllerShowTimeoutMs = 3_500
                            controllerAutoShow = entry.kind != MediaKind.LIVE
                            setOnClickListener {
                                if (entry.kind != MediaKind.LIVE) {
                                    controlsVisible = true
                                    showController()
                                }
                            }
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            keepScreenOn = true
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Live TV full-screen deliberately has no title badge, buttons, or other overlays.
            if (entry.kind != MediaKind.LIVE && controlsVisible) {
            Row(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.kind != MediaKind.LIVE) {
                    if (activeEngine == "exoplayer") OutlinedButton(onClick = { showAudioDialog = true }) { Text(appText("Audio tracks", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = { showOnlineSubtitleDialog = true }) { Text(appText("Online subtitles", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = { subtitlePicker.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream")) }) { Text(appText("Load subtitles", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = { externalAudioPicker.launch(arrayOf("audio/*", "application/octet-stream")) }) { Text(appText("External audio", LocalAppLanguage.current)) }
                }
                if (entry.kind == MediaKind.LIVE && qualityOptions.size > 1) {
                    OutlinedButton(onClick = { showQualityDialog = true }) { Text(appText("Choose quality", LocalAppLanguage.current)) }
                }
                if (entry.kind == MediaKind.LIVE && channels.isNotEmpty()) {
                    OutlinedButton(onClick = { showList = !showList }) { Text(if (showList) "Hide channels" else "Channels") }
                }
                OutlinedButton(onClick = onDismiss) { Text(appText("Close", LocalAppLanguage.current)) }
            }
            }
            if (entry.kind != MediaKind.LIVE && activeEngine == "mpv" && controlsVisible) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = {
                        val view = mpvView
                        if (view != null) view.timePos = ((view.timePos ?: 0.0) - 10.0).coerceAtLeast(0.0)
                    }) { Text("−10s") }
                    Button(onClick = {
                        val view = mpvView
                        if (view != null) {
                            view.paused = !(view.paused ?: false)
                            mpvPaused = view.paused ?: !mpvPaused
                        }
                    }) { Text(if (mpvPaused) "Resume" else "Pause") }
                    OutlinedButton(onClick = {
                        val view = mpvView
                        if (view != null) {
                            val current = view.timePos ?: 0.0
                            val duration = view.duration ?: Double.MAX_VALUE
                            view.timePos = (current + 10.0).coerceAtMost(duration)
                        }
                    }) { Text("+10s") }
                    OutlinedButton(onClick = {
                        runCatching { mpvView?.paused = true }
                        onDismiss()
                    }) { Text(appText("Stop", LocalAppLanguage.current)) }
                    OutlinedButton(onClick = { showMpvSettings = true }) { Text(appText("Player settings", LocalAppLanguage.current)) }
                }
            }
            if (showMpvSettings) {
                AlertDialog(
                    onDismissRequest = { showMpvSettings = false },
                    title = { Text(appText("Player settings", LocalAppLanguage.current)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HorizontalDivider()
                            Text(appText("Choose the playback engine or restore the default player.", LocalAppLanguage.current))
                            Text(appText("Current engine: MPV", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            preferences.setPlaybackEngine("exoplayer")
                            activeEngine = "exoplayer"
                            mpvPaused = false
                            showMpvSettings = false
                        }) { Text(appText("Default player (ExoPlayer)", LocalAppLanguage.current)) }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                mpvAttempt += 1
                                mpvPaused = false
                                showMpvSettings = false
                            }) { Text(appText("Restart MPV", LocalAppLanguage.current)) }
                            TextButton(onClick = { showMpvSettings = false }) { Text(appText("Close", LocalAppLanguage.current)) }
                        }
                    },
                )
            }
            if (showOnlineSubtitleDialog) {
                AlertDialog(
                    onDismissRequest = { if (!onlineSubtitleLoading) showOnlineSubtitleDialog = false },
                    title = { Text(appText("Search online subtitles", LocalAppLanguage.current)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = onlineSubtitleQuery, onValueChange = { onlineSubtitleQuery = it }, label = { Text(appText("Movie or series title", LocalAppLanguage.current)) }, singleLine = true)
                            OutlinedTextField(value = onlineSubtitleLanguage, onValueChange = { onlineSubtitleLanguage = it.take(2).lowercase() }, label = { Text(appText("Language code (ar, en, fr)", LocalAppLanguage.current)) }, singleLine = true)
                            Text(if (BuildConfig.OPEN_SUBTITLES_API_KEY.isBlank()) "OpenSubtitles API key is not configured in GitHub Actions." else "Search OpenSubtitles and download a matching subtitle into the player.", color = TextMuted, fontSize = 12.sp)
                            if (onlineSubtitleLoading) CircularProgressIndicator()
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = !onlineSubtitleLoading && BuildConfig.OPEN_SUBTITLES_API_KEY.isNotBlank() && onlineSubtitleQuery.isNotBlank(), onClick = {
                            onlineSubtitleLoading = true
                            playbackScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { downloadOnlineSubtitle(context.cacheDir, onlineSubtitleQuery, onlineSubtitleLanguage, BuildConfig.OPEN_SUBTITLES_API_KEY) }
                                }
                                result.onSuccess { file ->
                                    runCatching {
                                        val mime = if (file.extension.equals("vtt", true)) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                                        val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(file)).setMimeType(mime)
                                            .setLanguage(onlineSubtitleLanguage).setLabel("Online subtitle").build()
                                        val item = mediaItemForStream(entry.streamUrl, listOf(subtitle))
                                        val position = player.currentPosition.coerceAtLeast(0L)
                                        player.setMediaItem(item, position)
                                        player.prepare()
                                        player.playWhenReady = true
                                        subtitleMessage = "Online subtitle loaded. Enable it in the player's subtitle menu if needed."
                                        showOnlineSubtitleDialog = false
                                    }.onFailure { subtitleMessage = it.localizedMessage ?: "Subtitle downloaded but could not be loaded." }
                                }.onFailure { subtitleMessage = it.localizedMessage ?: "Could not find or download an online subtitle." }
                                onlineSubtitleLoading = false
                            }
                        }) { Text(appText("Search and load", LocalAppLanguage.current)) }
                    },
                    dismissButton = { TextButton(onClick = { showOnlineSubtitleDialog = false }, enabled = !onlineSubtitleLoading) { Text(appText("Cancel", LocalAppLanguage.current)) } },
                )
            }
            if (subtitleMessage.isNotBlank()) {
                Text(subtitleMessage, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .background(Ink.copy(alpha = 0.88f), RoundedCornerShape(8.dp)).padding(10.dp),
                    color = TextMain, fontSize = 12.sp)
            }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Lime)
            if (error.isNotBlank()) {
                Column(
                    Modifier.align(Alignment.Center).background(Ink.copy(alpha = 0.94f), RoundedCornerShape(10.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(appText("Playback unavailable", LocalAppLanguage.current), color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(error, color = TextMuted, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            error = ""
                            loading = true
                            if (activeEngine == "mpv") {
                                mpvAttempt++
                            } else {
                                player.prepare()
                                player.play()
                            }
                        }) { Text(appText("Retry", LocalAppLanguage.current)) }
                        OutlinedButton(onClick = {
                            val fallbackEngine = if (activeEngine == "mpv") "exoplayer" else "mpv"
                            preferences.setPlaybackEngine(fallbackEngine)
                            activeEngine = fallbackEngine
                            error = ""
                            loading = true
                            mpvPaused = false
                        }) { Text(if (activeEngine == "mpv") "Try default player" else "Try MPV") }
                    }
                }
            }
            if (showList && entry.kind != MediaKind.LIVE) {
                LazyColumn(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(330.dp)
                        .background(Ink.copy(alpha = 0.96f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                        Surface(
                            color = if (channel.id == entry.id) Raised else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().focusable().clickable { showList = false; onPlay(channel) },
                        ) {
                            Text(
                                "${(index + 1).toString().padStart(3, '0')}  ${channel.title}",
                                color = if (channel.id == entry.id) Lime else TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
    if (showAudioDialog) {
        val mappedInfo = trackSelector.currentMappedTrackInfo
        val audioChoices = buildList {
            if (mappedInfo != null) {
                for (rendererIndex in 0 until mappedInfo.rendererCount) {
                    if (mappedInfo.getRendererType(rendererIndex) != C.TRACK_TYPE_AUDIO) continue
                    val groups = mappedInfo.getTrackGroups(rendererIndex)
                    for (groupIndex in 0 until groups.length) {
                        val group = groups.get(groupIndex)
                        for (trackIndex in 0 until group.length) {
                            val format = group.getFormat(trackIndex)
                            val label = listOfNotNull(format.label?.takeIf { it.isNotBlank() }, format.language?.takeIf { it.isNotBlank() }, format.sampleMimeType?.substringAfter('/')?.takeIf { it.isNotBlank() }).distinct().joinToString(" · ").ifBlank { "Audio track ${trackIndex + 1}" }
                            add(Triple(group, trackIndex, label))
                        }
                    }
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            title = { Text(appText("Audio tracks", LocalAppLanguage.current)) },
            text = {
                if (audioChoices.isEmpty()) Text(appText("No selectable audio tracks are available yet, or this stream is still loading.", LocalAppLanguage.current))
                else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    audioChoices.forEachIndexed { index, choice ->
                        TextButton(onClick = {
                            trackSelector.parameters = trackSelector.buildUponParameters()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .addOverride(TrackSelectionOverride(choice.first, listOf(choice.second)))
                                .build()
                            showAudioDialog = false
                        }) { Text(choice.third.ifBlank { "Audio ${index + 1}" }) }
                    }
                    TextButton(onClick = {
                        trackSelector.parameters = trackSelector.buildUponParameters().clearOverridesOfType(C.TRACK_TYPE_AUDIO).build()
                        showAudioDialog = false
                    }) { Text(appText("Automatic selection", LocalAppLanguage.current)) }
                }
            },
            confirmButton = { TextButton(onClick = { showAudioDialog = false }) { Text(appText("Close", LocalAppLanguage.current)) } },
        )
    }
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(appText("Choose stream quality", LocalAppLanguage.current)) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(qualityOptions, key = { it.id }) { option ->
                        Surface(
                            color = if (option.id == entry.id) Raised else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                showQualityDialog = false
                                onPlay(option)
                            },
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(option.title, color = if (option.id == entry.id) Lime else TextMain, fontWeight = FontWeight.SemiBold)
                                Text(qualityLabel(option).ifBlank { "Quality not identified" }, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQualityDialog = false }) { Text(appText("Done", LocalAppLanguage.current)) } },
        )
    }
}

@Composable
private fun AddManualChannelDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var logo by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Custom") }
    var error by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appText("إضافة قناة يدوياً", LocalAppLanguage.current)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text(appText("اسم القناة", LocalAppLanguage.current)) }, singleLine = true)
                OutlinedTextField(url, { url = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text(appText("رابط البث", LocalAppLanguage.current)) }, singleLine = true)
                OutlinedTextField(logo, { logo = it }, modifier = Modifier.fillMaxWidth(), label = { Text(appText("رابط الشعار (اختياري)", LocalAppLanguage.current)) }, singleLine = true)
                OutlinedTextField(category, { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text(appText("التصنيف", LocalAppLanguage.current)) }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    title.isBlank() -> error = "دخل اسم القناة."
                    !(url.trim().startsWith("http://", true) || url.trim().startsWith("https://", true)) -> error = "دخل رابط بث صحيح يبدأ بـ http أو https."
                    else -> onSave(title.trim(), url.trim(), logo.trim(), category.trim().ifBlank { "Custom" })
                }
            }) { Text(appText("إضافة", LocalAppLanguage.current)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(appText("إلغاء", LocalAppLanguage.current)) } },
    )
}

@Composable
private fun OrganizeChannelDialog(
    channel: MediaEntry,
    channels: List<MediaEntry>,
    preferences: PlayerPreferences,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String) -> Unit,
) {
    var title by rememberSaveable(channel.id) { mutableStateOf(preferences.channelTitle(channel.id, channel.title)) }
    var category by rememberSaveable(channel.id) { mutableStateOf(preferences.categoryFor(channel.id, channel.category)) }
    var streamUrl by rememberSaveable(channel.id) { mutableStateOf(channel.streamUrl) }
    var logoUrl by rememberSaveable(channel.id) { mutableStateOf(channel.logoUrl) }
    var channelNumber by rememberSaveable(channel.id) { mutableStateOf(channelNumber(channel, channels, preferences).toString()) }
    var newCategory by rememberSaveable(channel.id) { mutableStateOf("") }
    var error by rememberSaveable(channel.id) { mutableStateOf("") }
    val categories = (AUTO_CHANNEL_CATEGORIES + channels.map { automaticChannelCategory(it) } +
        channels.map { it.category } + preferences.customCategories()).filter(String::isNotBlank).distinct().sorted()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appText("Edit channel", LocalAppLanguage.current)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("Channel name", LocalAppLanguage.current)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = channelNumber,
                    onValueChange = { channelNumber = it.filter(Char::isDigit).take(5); error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("Channel number", LocalAppLanguage.current)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (channel.id.startsWith("manual:")) {
                    OutlinedTextField(streamUrl, { streamUrl = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text(appText("رابط البث", LocalAppLanguage.current)) }, singleLine = true)
                    OutlinedTextField(logoUrl, { logoUrl = it; error = "" }, modifier = Modifier.fillMaxWidth(), label = { Text(appText("رابط الشعار (اختياري)", LocalAppLanguage.current)) }, singleLine = true)
                }
                Text(appText("Move to category", LocalAppLanguage.current), color = TextMuted, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(categories, key = { it }) { option ->
                        CategoryPill(option, category == option) { category = option }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        newCategory,
                        { newCategory = it; error = "" },
                        label = { Text(appText("New category", LocalAppLanguage.current)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        if (!preferences.addCategory(newCategory)) error = "Enter a new category name."
                        else {
                            category = newCategory.trim()
                            newCategory = ""
                        }
                    }) { Text(appText("Create", LocalAppLanguage.current)) }
                }
                Text(appText("If the number is in use, the two channels will swap numbers.", LocalAppLanguage.current), color = TextMuted, fontSize = 10.sp)
                if (error.isNotBlank()) Text(error, color = ErrorRed, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val number = channelNumber.toIntOrNull()
                when {
                    title.isBlank() -> error = "Enter a channel name."
                    category.isBlank() -> error = "Choose a category."
                    number == null || number < 1 -> error = "Enter a channel number greater than zero."
                    channel.id.startsWith("manual:") && !(streamUrl.trim().startsWith("http://", true) || streamUrl.trim().startsWith("https://", true)) -> error = "Enter a valid stream URL."
                    else -> onSave(title.trim(), category, number, streamUrl.trim(), logoUrl.trim())
                }
            }) { Text(appText("Save", LocalAppLanguage.current)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(appText("Cancel", LocalAppLanguage.current)) } },
    )
}

@Composable
private fun LoadingState(label: String, progress: Float? = null, loadedItems: Int = 0) {
    if (progress == null) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = Lime)
            Text(label, color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(350), label = "catalog-cup-fill")
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.width(108.dp).height(142.dp), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 4.dp)
                .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 8.dp, topEnd = 8.dp))
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp, topStart = 5.dp, topEnd = 5.dp))) {
                Box(Modifier.fillMaxWidth().fillMaxHeight(animatedProgress).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(GoldLight, Lime, Color(0xFFB77D17)))))
            }
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent,
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 8.dp, topEnd = 8.dp),
                border = BorderStroke(3.dp, GoldLight.copy(alpha = 0.9f))) {}
            Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.TopCenter).padding(horizontal = 1.dp).background(GoldLight, RoundedCornerShape(50)))
        }
        Spacer(Modifier.height(22.dp))
        Text(label, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("${(animatedProgress * 100).toInt()}%", color = GoldLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (loadedItems > 0) Text("$loadedItems ${appText("items loaded", LocalAppLanguage.current)}", color = TextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(appText("Please wait until the catalogue is ready", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = TextMuted, fontSize = 12.sp)
    }
}

private val AUTO_CHANNEL_CATEGORIES = listOf(
    "Sports", "News", "Movies", "Entertainment", "Kids"
)

/** Lightweight, offline classification. Explicit user/provider categories remain available as fallbacks. */
private fun automaticChannelCategory(channel: MediaEntry): String {
    val text = "${channel.title} ${channel.category} ${channel.genre}".lowercase()
    fun hasAny(vararg terms: String) = terms.any { term -> text.contains(term) }
    return when {
        hasAny("bein sport", "beinsports", "sky sport", "sport tv", "sports", "sport", "espn", "dazn",
            "ssc", "riyadh", "football", "soccer", "match", "sport", "رياضة", "رياضي", "مباريات",
            "الكأس", "الدوري", "كرة القدم", "مباراة") -> "Sports"
        hasAny("news", "breaking", "cnn", "bbc news", "al jazeera", "aljazeera", "france 24",
            "sky news", "العربية", "الجزيرة", "الأخبار", "اخبار", "خبر") -> "News"
        hasAny("kids", "cartoon", "nickelodeon", "nick jr", "baby tv", "disney junior",
            "cartoon network", "spacetoon", "mbc3", "jeem", "أطفال", "اطفال", "كرتون", "براعم") -> "Kids"
        hasAny("movie", "cinema", "film", "hbo", "hollywood", "movies", "ciné", "cinema",
            "أفلام", "افلام", "فيلم", "سينما") -> "Movies"
        hasAny("entertainment", "music", "comedy", "drama", "lifestyle", "variety",
            "ترفيه", "موسيقى", "دراما", "منوعات") -> "Entertainment"
        else -> channel.category.trim().ifBlank { "Other" }
    }
}

private fun visibleChannels(
    catalog: SourceCatalog,
    category: String,
    search: String,
    preferences: PlayerPreferences,
): List<MediaEntry> = numberedChannels(catalog.liveChannels, preferences).map { it.first }.filter { channel ->
    val advertisingChannel = channel.title.contains("####") ||
        preferences.channelTitle(channel.id, channel.title).contains("####")
    val hidden = channel.id in preferences.hiddenChannels()
    val automaticCategory = automaticChannelCategory(channel)
    val effectiveCategory = preferences.categoryFor(channel.id, automaticCategory)
    !advertisingChannel &&
        (when (category) {
            "All" -> !hidden
            "Favorites" -> !hidden && channel.id in preferences.favorites()
            "Hidden" -> hidden
            // Provider categories must remain selectable even when automatic
            // classification (for example Sports) assigns a different display group.
            else -> !hidden && (
                effectiveCategory.equals(category, ignoreCase = true) ||
                    channel.category.trim().equals(category, ignoreCase = true)
                )
        }) &&
        (search.isBlank() || channel.title.contains(search, true) || channel.category.contains(search, true))
}

@Composable
private fun KidsPosterShelf(
    title: String,
    entries: List<MediaEntry>,
    section: Section,
    compact: Boolean,
    accent: Color,
    onPlay: (MediaEntry) -> Unit,
    onPlayYoutube: (String) -> Unit,
    onMovieDetails: (MediaEntry) -> Unit,
) {
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = accent, fontSize = if (compact) 15.sp else 19.sp, fontWeight = FontWeight.Black)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 10.dp)) {
            items(entries.distinctBy { it.id }, key = { "kids-shelf:" + section.name + ":" + it.id }) { entry ->
                Column(
                    Modifier.width(if (compact) 112.dp else 148.dp).clickable {
                        if (entry.id.startsWith("youtube:")) onPlayYoutube(entry.id.removePrefix("youtube:"))
                        else if (section == Section.MOVIES) onMovieDetails(entry) else onPlay(entry)
                    },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        color = Color(0xFF24456B),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.75f)),
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.70f),
                    ) {
                        Box {
                            if (entry.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = entry.logoUrl,
                                    contentDescription = entry.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF7256D8), Color(0xFF22A9B8)))),
                                    contentAlignment = Alignment.Center,
                                ) { Text("🧸\n⭐", fontSize = if (compact) 28.sp else 36.sp, lineHeight = 40.sp) }
                            }
                            Box(
                                Modifier.align(Alignment.BottomEnd).padding(5.dp)
                                    .background(Color(0xCC151B3B), RoundedCornerShape(50))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) { Text("▶ شاهد", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text(entry.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 11.sp else 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (entry.year.isNotBlank()) Text(entry.year, color = Color(0xFFB6D5E5), fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun KidsDashboard(
    compact: Boolean,
    section: Section,
    movies: List<MediaEntry>,
    series: List<MediaEntry>,
    channels: List<MediaEntry>,
    search: String,
    onSearch: (String) -> Unit,
    onSection: (Section) -> Unit,
    onPlay: (MediaEntry) -> Unit,
    onPlayYoutube: (String) -> Unit,
    onMovieDetails: (MediaEntry) -> Unit,
    onExit: () -> Unit,
    onLoadMore: () -> Unit,
) {
    var youtubeResults by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var youtubeResultsForQuery by remember { mutableStateOf("") }
    var youtubeResultsForSection by remember { mutableStateOf<Section?>(null) }
    var youtubeLoading by remember { mutableStateOf(false) }
    var youtubeError by remember { mutableStateOf("") }

    LaunchedEffect(search, section) {
        val query = search.trim()
        if (query.length < 2 || section == Section.LIVE) {
            youtubeResults = emptyList()
            youtubeResultsForQuery = ""
            youtubeResultsForSection = null
            youtubeError = ""
            youtubeLoading = false
        } else {
            delay(450)
            youtubeLoading = true
            youtubeError = ""
            try {
                val results = withContext(Dispatchers.IO) { runInterruptible { searchYouTubeKids(query, section) } }
                currentCoroutineContext().ensureActive()
                youtubeResults = results
                youtubeResultsForQuery = query
                youtubeResultsForSection = section
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                youtubeResults = emptyList()
                youtubeError = failure.message ?: "تعذر تحميل نتائج YouTube"
            } finally {
                youtubeLoading = false
            }
        }
    }

    val source = when (section) {
        Section.LIVE -> channels
        Section.SERIES -> series.filter { it.kind == MediaKind.SERIES }
        else -> movies.filter { it.kind == MediaKind.MOVIE }
    }
    val activeYoutubeResults = remember(youtubeResults, youtubeResultsForQuery, youtubeResultsForSection, search, section) {
        if (search.trim().length >= 2 && youtubeResultsForQuery == search.trim() && youtubeResultsForSection == section) {
            youtubeResults.filter { result ->
                when (section) {
                    Section.SERIES -> result.kind == MediaKind.SERIES
                    Section.MOVIES -> result.kind == MediaKind.MOVIE
                    else -> false
                }
            }
        } else emptyList()
    }
    val visible = remember(source, search, activeYoutubeResults, section) {
        val local = source.filter {
            search.isBlank() || it.title.contains(search, ignoreCase = true) ||
                it.category.contains(search, ignoreCase = true) || it.genre.contains(search, ignoreCase = true)
        }
        if (search.isBlank() || section == Section.LIVE) local else (local + activeYoutubeResults).distinctBy { it.id }
    }
    val kidAccent = when (section) {
        Section.SERIES -> Color(0xFFB99AFF)
        Section.LIVE -> Color(0xFF73E0FF)
        else -> Color(0xFFFFC857)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF102E50), Color(0xFF151B3B), Color(0xFF301D4D))))) {
        KidsBackgroundDoodles(compact = compact)
        when (section) {
            Section.LIVE -> {
                val gridState = rememberLazyGridState()
                LaunchedEffect(gridState, channels.size) {
                    snapshotFlow {
                        val info = gridState.layoutInfo
                        Pair(info.visibleItemsInfo.lastOrNull()?.index ?: -1, info.totalItemsCount)
                    }.distinctUntilChanged().collectLatest { (lastVisible, total) ->
                        if (total > 0 && lastVisible >= total - 6) onLoadMore()
                    }
                }
                if (channels.isEmpty()) {
                    LaunchedEffect(section, channels.size) { onLoadMore() }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📺", fontSize = 42.sp)
                            Text("كنحمّلو قنوات الأطفال…", color = Color.White, fontSize = 15.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (compact) 112.dp else 148.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = if (compact) 7.dp else 14.dp, vertical = if (compact) 7.dp else 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 12.dp),
                    ) {
                        gridItems(channels.distinctBy { it.id }, key = { "kids-live:" + it.id }) { channel ->
                            Column(Modifier.fillMaxWidth().clickable { onPlay(channel) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = Color(0xFF24456B), shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, kidAccent.copy(alpha = 0.75f)),
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.82f),
                                ) {
                                    Box {
                                        if (channel.logoUrl.isNotBlank()) {
                                            AsyncImage(model = channel.logoUrl, contentDescription = channel.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📺", fontSize = 42.sp) }
                                        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color(0xD9151B3B)).padding(horizontal = 7.dp, vertical = 5.dp)) {
                                            Text(channel.title, color = Color.White, fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Section.MOVIES, Section.SERIES -> {
                val listState = rememberLazyListState()
                val providerEntries = remember(source) { source.filterNot { it.id.startsWith("youtube:") } }
                val youtubeTrendEntries = remember(source) { source.filter { it.id.startsWith("youtube:") } }
                val newestProviderFirst = remember(providerEntries) {
                    providerEntries.sortedByDescending { (it.year.ifBlank { it.releaseDate.take(4) }).toIntOrNull() ?: 0 }
                }
                // Always reserve visible places for YouTube trend videos so a large provider
                // catalogue cannot push every YouTube result below the first shelf.
                val trending = remember(newestProviderFirst, youtubeTrendEntries) {
                    (newestProviderFirst.take(10) + youtubeTrendEntries.take(10)).distinctBy { it.id }
                }
                val trendingIds = remember(trending) { trending.mapTo(mutableSetOf()) { it.id } }
                val recommended = remember(providerEntries, youtubeTrendEntries, trendingIds, trending) {
                    val providerRecommended = providerEntries.filterNot { it.id in trendingIds }
                        .sortedWith(compareBy<MediaEntry> { it.category.lowercase() }.thenBy { it.title.lowercase() })
                        .take(12)
                    val youtubeRecommended = youtubeTrendEntries.filterNot { it.id in trendingIds }.take(4)
                    (providerRecommended + youtubeRecommended).ifEmpty { trending.take(10) }
                }
                LaunchedEffect(listState, section, visible.size, search) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .distinctUntilChanged().collectLatest { lastVisible ->
                            val total = listState.layoutInfo.totalItemsCount
                            if (total > 0 && lastVisible >= total - 3) onLoadMore()
                        }
                }
                Column(Modifier.fillMaxSize().padding(horizontal = if (compact) 7.dp else 14.dp, vertical = if (compact) 6.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = search, onValueChange = onSearch,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 58.dp),
                        singleLine = true, label = { Text("🔎 قلب على فيلم أو كرتون أو مسلسل", fontSize = 11.sp) },
                    )
                    if (youtubeLoading || youtubeError.isNotBlank()) {
                        Text(
                            if (youtubeLoading) "كنقلبو فـ YouTube…" else "تعذر بحث YouTube: $youtubeError",
                            color = if (youtubeError.isNotBlank()) Color(0xFFFFC0B0) else Color(0xFFE5F4FF),
                            fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (visible.isEmpty()) {
                        if (source.isEmpty()) LaunchedEffect(section, source.size) { onLoadMore() }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (youtubeLoading) "كنقلبو ليك على محتوى مناسب…"
                                else if (search.isBlank()) "كنحمّلو المحتوى…"
                                else "ما لقيناش نتائج، جرّب كلمة أخرى 😊",
                                color = Color(0xFFE5F4FF),
                            )
                        }
                    } else {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (search.isNotBlank()) {
                                item(key = "kids-search-results") {
                                    KidsPosterShelf("نتائج البحث", visible, section, compact, kidAccent, onPlay, onPlayYoutube, onMovieDetails)
                                }
                            } else {
                                item(key = "kids-trending") {
                                    KidsPosterShelf(appText("🔥 Trending", LocalAppLanguage.current), trending, section, compact, kidAccent, onPlay, onPlayYoutube, onMovieDetails)
                                }
                                item(key = "kids-recommended") {
                                    KidsPosterShelf(appText("⭐ Recommended", LocalAppLanguage.current), recommended, section, compact, kidAccent, onPlay, onPlayYoutube, onMovieDetails)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize().padding(if (compact) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(appText("🌈 TVI KIDS", LocalAppLanguage.current), color = Color(0xFFFFD76A), fontSize = if (compact) 23.sp else 34.sp, fontWeight = FontWeight.Black)
                            Text(appText("اختار شنو بغيتي تتفرج اليوم ⭐", LocalAppLanguage.current), color = Color(0xFFE5F4FF), fontSize = if (compact) 11.sp else 14.sp)
                        }
                        OutlinedButton(onClick = onExit, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) {
                            Text(appText("👨 Parent / PIN", LocalAppLanguage.current), fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    Surface(
                        color = Color(0xFF193B62), shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(2.dp, Color(0xFF73E0FF)),
                        modifier = Modifier.fillMaxWidth().weight(1.05f).clickable { onSection(Section.LIVE) },
                    ) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("📺", fontSize = if (compact) 42.sp else 64.sp)
                                Text(appText("TV LIVE", LocalAppLanguage.current), color = Color.White, fontSize = if (compact) 25.sp else 36.sp, fontWeight = FontWeight.Black)
                                Text(appText("قنوات الأطفال مباشرة", LocalAppLanguage.current), color = Color(0xFFD7F4FF), fontSize = if (compact) 11.sp else 15.sp)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().weight(0.78f), horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
                        Surface(
                            color = Color(0xFF6546B8), shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(2.dp, Color(0xFFFFC857)),
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSection(Section.MOVIES) },
                        ) {
                            Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🎨", fontSize = if (compact) 34.sp else 52.sp)
                                    Text(appText("الأفلام", LocalAppLanguage.current), color = Color.White, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.Black)
                                    Text(appText("Movies", LocalAppLanguage.current), color = Color(0xFFFFEAB0), fontSize = 12.sp)
                                }
                            }
                        }
                        Surface(
                            color = Color(0xFF24456B), shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(2.dp, Color(0xFFB99AFF)),
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSection(Section.SERIES) },
                        ) {
                            Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🦸", fontSize = if (compact) 34.sp else 52.sp)
                                    Text(appText("المسلسلات", LocalAppLanguage.current), color = Color.White, fontSize = if (compact) 20.sp else 28.sp, fontWeight = FontWeight.Black)
                                    Text(appText("Series", LocalAppLanguage.current), color = Color(0xFFE8DFFF), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/** Matches provider category labels that explicitly identify child/family content. */
private fun isKidsCategoryName(value: String): Boolean {
    val normalized = value.lowercase()
    return listOf(
        "kids", "kid", "children", "child", "junior", "cartoon", "animation",
        "baby", "preschool", "family", "disney junior", "nick jr", "nickelodeon",
        "spacetoon", "mbc3", "jeem", "براعم", "أطفال", "اطفال", "طفل",
        "كرتون", "رسوم متحركة", "العائلة", "عائلي",
    ).any { normalized.contains(it) }
}

/** YouTube Data API search sorted by view count with strict SafeSearch. */
private fun searchYouTubeKidsTrending(section: Section): List<MediaEntry> {
    require(section == Section.MOVIES || section == Section.SERIES)
    val apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
    if (apiKey.isBlank()) return emptyList()
    val query = if (section == Section.SERIES) {
        "kids cartoon series episodes animation nursery rhymes family friendly"
    } else {
        "kids cartoon movie full movie animation family friendly"
    }
    val params = listOf(
        "part" to "snippet",
        "q" to query,
        "type" to "video",
        "videoEmbeddable" to "true",
        "order" to "viewCount",
        "maxResults" to "25",
        "safeSearch" to "strict",
        "regionCode" to "MA",
        "relevanceLanguage" to "en",
        "key" to apiKey,
    ).joinToString("&") { (key, value) ->
        java.net.URLEncoder.encode(key, "UTF-8") + "=" + java.net.URLEncoder.encode(value, "UTF-8")
    }
    val connection = (URL("https://www.googleapis.com/youtube/v3/search?$params").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 12000
        setRequestProperty("Accept", "application/json")
    }
    return try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) return emptyList()
        val items = JSONObject(body).optJSONArray("items") ?: return emptyList()
        (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
            val snippet = item.optJSONObject("snippet") ?: return@mapNotNull null
            val title = snippet.optString("title").trim()
            if (videoId.isBlank() || title.isBlank()) return@mapNotNull null
            val thumbnails = snippet.optJSONObject("thumbnails")
            val thumbnail = thumbnails?.optJSONObject("high")?.optString("url").orEmpty()
                .ifBlank { thumbnails?.optJSONObject("medium")?.optString("url").orEmpty() }
                .ifBlank { thumbnails?.optJSONObject("default")?.optString("url").orEmpty() }
            MediaEntry(
                id = "youtube:" + videoId,
                title = title,
                category = if (section == Section.SERIES) "YouTube Kids • مسلسل" else "YouTube Kids • فيلم وكرتون",
                streamUrl = "https://www.youtube.com/watch?v=" + videoId,
                logoUrl = thumbnail.ifBlank { "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg" },
                kind = if (section == Section.SERIES) MediaKind.SERIES else MediaKind.MOVIE,
                description = "YouTube Kids trending result • " + snippet.optString("channelTitle"),
            )
        }.distinctBy { it.id }.take(20)
    } finally {
        connection.disconnect()
    }
}

private fun searchYouTubeTrailer(entry: MediaEntry): String? {
    val apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
    if (apiKey.isBlank()) throw IllegalStateException("YouTube API key is not configured.")
    val canonicalTitle = entry.title.trim().replace(Regex("\\s+"), " ")
    if (canonicalTitle.length < 2) return null
    val year = entry.year.ifBlank { entry.releaseDate.take(4) }.filter(Char::isDigit).take(4)
    val query = buildList {
        add(canonicalTitle)
        if (year.isNotBlank()) add(year)
        if (entry.genre.isNotBlank()) add(entry.genre.split(",", "|", "/", "·").first().trim())
        add(if (entry.kind == MediaKind.SERIES) "official series trailer" else "official movie trailer")
    }.joinToString(" ")
    val params = listOf(
        "part" to "snippet",
        "q" to query,
        "type" to "video",
        "videoEmbeddable" to "true",
        "maxResults" to "15",
        "safeSearch" to "strict",
        "regionCode" to "MA",
        "relevanceLanguage" to "en",
        "key" to apiKey,
    ).joinToString("&") { (key, value) ->
        java.net.URLEncoder.encode(key, "UTF-8") + "=" + java.net.URLEncoder.encode(value, "UTF-8")
    }
    val connection = (URL("https://www.googleapis.com/youtube/v3/search?$params").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
    }
    return try {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val apiMessage = runCatching { JSONObject(body).optJSONObject("error")?.optString("message").orEmpty() }.getOrDefault("")
            throw IOException(apiMessage.ifBlank { "YouTube trailer search failed (HTTP $code)." })
        }
        val items = JSONObject(body).optJSONArray("items") ?: return null
        val normalizedTitle = canonicalTitle.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
        val titleTokens = normalizedTitle.split(" ").filter { it.length > 2 }.toSet()
        if (titleTokens.isEmpty()) return null
        val candidates = (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
            val snippet = item.optJSONObject("snippet") ?: return@mapNotNull null
            val candidateTitle = snippet.optString("title").lowercase()
            if (id.isBlank() || !(candidateTitle.contains("trailer") || candidateTitle.contains("teaser"))) return@mapNotNull null
            val normalizedCandidate = candidateTitle.replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
            val candidateTokens = normalizedCandidate.split(" ").toSet()
            val overlap = titleTokens.count { it in candidateTokens }
            val titleScore = overlap.toFloat() / titleTokens.size.toFloat()
            val yearMatches = year.isBlank() || candidateTitle.contains(year)
            val seriesMatches = entry.kind != MediaKind.SERIES ||
                listOf("series", "season", "official trailer", "tv show").any { candidateTitle.contains(it) }
            if (titleScore < 0.75f || !yearMatches || !seriesMatches) return@mapNotNull null
            Triple(id, titleScore + if (yearMatches && year.isNotBlank()) 0.1f else 0f, snippet.optString("channelTitle"))
        }
        candidates.maxByOrNull { it.second }?.first
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun YouTubeTrailerDialog(videoId: String, onDismiss: () -> Unit) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = WebViewClient()
                        webViewRef = this
                        val html = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1"><style>html,body,iframe{margin:0;width:100%;height:100%;background:#000;border:0;overflow:hidden}</style></head><body><iframe src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=0&modestbranding=1&rel=0&playsinline=1" allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></body></html>"""
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "UTF-8", null)
                    }
                },
                update = { webViewRef = it },
                onRelease = { it.stopLoading(); it.destroy(); if (webViewRef === it) webViewRef = null },
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) { Text(appText("Close", LocalAppLanguage.current), color = Color.White) }
        }
    }
}

@Composable
private fun KidsYouTubePlayerDialog(videoId: String, onDismiss: () -> Unit) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            factory = { context ->
                val root = android.widget.FrameLayout(context).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                            if (view == null) return
                            customViewCallback?.onCustomViewHidden()
                            customView = view
                            customViewCallback = callback
                            this@apply.visibility = android.view.View.GONE
                            (view.parent as? android.view.ViewGroup)?.removeView(view)
                            root.addView(view, android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            ))
                            val activity = generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
                                .filterIsInstance<Activity>().firstOrNull()
                            activity?.window?.decorView?.systemUiVisibility =
                                (android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                        }

                        override fun onHideCustomView() {
                            customView?.let { root.removeView(it) }
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            this@apply.visibility = android.view.View.VISIBLE
                        }
                    }
                    webViewRef = this
                }
                root.addView(webView, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                ))
                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                      <style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
                      iframe{position:absolute;inset:0;width:100%;height:100%;border:0}</style>
                    </head>
                    <body>
                      <iframe
                        src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=0&fs=1&controls=1&rel=0&origin=https%3A%2F%2Fwww.youtube-nocookie.com"
                        title="YouTube video player"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowfullscreen>
                      </iframe>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL("https://www.youtube-nocookie.com/", html, "text/html", "UTF-8", null)
                root
            },
            onRelease = { root ->
                val activity = generateSequence(root.context) { (it as? android.content.ContextWrapper)?.baseContext }
                    .filterIsInstance<Activity>().firstOrNull()
                activity?.window?.decorView?.systemUiVisibility = 0
                webViewRef?.let { view ->
                    view.stopLoading()
                    view.webChromeClient = null
                    view.webViewClient = WebViewClient()
                    view.destroy()
                }
                webViewRef = null
                customView = null
                customViewCallback = null
            },
        )
    }
}

private fun searchYouTubeKids(query: String, section: Section): List<MediaEntry> {
    require(section == Section.MOVIES || section == Section.SERIES) {
        "YouTube search is only available in the Kids movies and series sections."
    }
    val apiKey = BuildConfig.YOUTUBE_API_KEY.trim()
    if (apiKey.isBlank()) {
        throw IllegalStateException("YouTube API key is not configured. Add YOUTUBE_API_KEY to your local Gradle properties.")
    }

    val params = listOf(
        "part" to "snippet",
        "q" to when (section) {
            Section.SERIES -> "$query kids cartoon series episode"
            else -> "$query kids cartoon movie animation"
        },
        "type" to "video",
        "maxResults" to "20",
        "safeSearch" to "strict",
        "regionCode" to "MA",
        "relevanceLanguage" to "ar",
        "key" to apiKey,
    ).joinToString("&") { (key, value) ->
        java.net.URLEncoder.encode(key, "UTF-8") + "=" + java.net.URLEncoder.encode(value, "UTF-8")
    }
    val connection = (URL("https://www.googleapis.com/youtube/v3/search?$params").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
    }

    return try {
        val responseCode = connection.responseCode
        val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) {
            val apiMessage = runCatching {
                JSONObject(responseText).optJSONObject("error")?.optString("message").orEmpty()
            }.getOrDefault("")
            throw IllegalStateException(
                if (apiMessage.isNotBlank()) "YouTube API: $apiMessage"
                else "YouTube API request failed (HTTP $responseCode)"
            )
        }

        val items = JSONObject(responseText).optJSONArray("items") ?: return emptyList()
        val found = mutableListOf<MediaEntry>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
            val snippet = item.optJSONObject("snippet") ?: continue
            val title = snippet.optString("title").trim()
            if (videoId.isBlank() || title.isBlank()) continue

            // Search snippets do not expose a reliable movie/series type, so reject only
            // strong cross-format signals instead of guessing from broad genre labels.
            val normalizedTitle = title.lowercase()
            val episodeMarker = Regex("""\b(?:s\d{1,2}\s*e\d{1,2}|season\s*\d+|episode\s*\d+|ep\s*\d+)\b""")
                .containsMatchIn(normalizedTitle) ||
                listOf("الحلقة", "الحلقات", "الموسم", "مسلسل").any { it in normalizedTitle }
            val movieMarker = Regex("""\b(?:full\s+movie|movie|film)\b""").containsMatchIn(normalizedTitle) ||
                listOf("فيلم", "أفلام", "افلام").any { it in normalizedTitle }
            if (section == Section.MOVIES && episodeMarker && !movieMarker) continue
            if (section == Section.SERIES && movieMarker) continue

            val thumbnails = snippet.optJSONObject("thumbnails")
            val thumbnail = thumbnails?.optJSONObject("high")?.optString("url").orEmpty()
                .ifBlank { thumbnails?.optJSONObject("medium")?.optString("url").orEmpty() }
                .ifBlank { thumbnails?.optJSONObject("default")?.optString("url").orEmpty() }

            found += MediaEntry(
                id = "youtube:" + videoId,
                title = title,
                category = if (section == Section.SERIES) "YouTube Kids • مسلسل" else "YouTube Kids • فيلم وكرتون",
                streamUrl = "https://www.youtube.com/watch?v=" + videoId,
                logoUrl = thumbnail.ifBlank { "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg" },
                kind = if (section == Section.SERIES) MediaKind.SERIES else MediaKind.MOVIE,
                description = "YouTube Kids result • " + snippet.optString("channelTitle"),
            )
        }
        found.distinctBy { it.id }.take(20)
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun YouTubeKidsSearchDialog(onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var submittedQuery by rememberSaveable { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            color = Color(0xFF101B36),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color(0xFFFFD76A)),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(appText("▶ YouTube", LocalAppLanguage.current), color = Color(0xFFFFD76A), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(appText("قلب وشاهد الفيديو هنا داخل TVI PLAYER", LocalAppLanguage.current), color = Color(0xFFE5F4FF), fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("رجوع ✕", color = Color.White) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("شنو بغيتي تتفرج؟") },
                    )
                    Button(
                        onClick = {
                            submittedQuery = query.trim()
                            val target = Uri.parse("https://m.youtube.com/").buildUpon()
                                .appendPath("search")
                                .appendQueryParameter("q", submittedQuery)
                                .build()
                            webViewRef?.loadUrl(target.toString())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7657D7), contentColor = Color.White),
                    ) { Text("بحث") }
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.White),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            webViewClient = WebViewClient()
                            webViewRef = this
                            loadUrl("https://m.youtube.com/")
                        }
                    },
                    update = { view ->
                        webViewRef = view
                        if (submittedQuery.isNotBlank()) {
                            val expected = Uri.parse("https://m.youtube.com/").buildUpon()
                                .appendPath("search")
                                .appendQueryParameter("q", submittedQuery)
                                .build().toString()
                            if (view.url != expected) view.loadUrl(expected)
                        }
                    },
                    onRelease = { view ->
                        view.stopLoading()
                        view.destroy()
                        if (webViewRef === view) webViewRef = null
                    },
                )
            }
        }
    }
}

@Composable
private fun KidsBackgroundDoodles(compact: Boolean) {
    // Decorative, low-contrast cartoon-style doodles stay behind all interactive content.
    val size = if (compact) 34.sp else 52.sp
    Box(Modifier.fillMaxSize()) {
        Text("🐭", modifier = Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 62.dp), fontSize = size, color = Color.White.copy(alpha = 0.13f))
        Text("🦄", modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp, top = 36.dp), fontSize = size, color = Color.White.copy(alpha = 0.12f))
        Text("🐻", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 50.dp), fontSize = size, color = Color.White.copy(alpha = 0.12f))
        Text("🚀", modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 38.dp), fontSize = size, color = Color.White.copy(alpha = 0.13f))
        Text("🌈", modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 92.dp), fontSize = size, color = Color.White.copy(alpha = 0.10f))
        Text("⭐", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp, bottom = 46.dp), fontSize = size, color = Color.White.copy(alpha = 0.12f))
        Text("🐼", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp, end = 150.dp), fontSize = size, color = Color.White.copy(alpha = 0.10f))
        Text("🦊", modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 190.dp), fontSize = size, color = Color.White.copy(alpha = 0.10f))
    }
}

/** Applies the device-local Kids Mode gate to every loaded catalogue page. */
private fun applyKidsModeFilter(entries: List<MediaEntry>, preferences: PlayerPreferences): List<MediaEntry> {
    if (!preferences.kidsModeEnabled()) return entries

    // Read the parent rules once per catalogue pass. Reading and parsing both JSON
    // preference arrays for every single item can stall/crash on very large IPTV lists.
    val allowedIds = preferences.kidsAllowedItemIds()
    val blockedIds = preferences.kidsBlockedItemIds()
    val childSafeTerms = listOf(
        "kids", "children", "educational", "education", "junior", "preschool",
        "toddler", "disney junior", "nick jr", "طفل", "أطفال", "براعم",
    )
    return entries.filter { entry ->
        val id = entry.id.trim()
        if (id.isEmpty() || id in blockedIds) {
            false
        } else if (id in allowedIds) {
            true
        } else {
            val searchable = (entry.title + " " + entry.category).lowercase()
            childSafeTerms.any { it in searchable }
        }
    }
}

private fun numberedChannels(channels: List<MediaEntry>, preferences: PlayerPreferences): List<Pair<MediaEntry, Int>> =
    channels.map { it to preferences.channelNumber(it.id) }.sortedBy { it.second }

private fun channelNumber(entry: MediaEntry, channels: List<MediaEntry>, preferences: PlayerPreferences): Int =
    numberedChannels(channels, preferences).firstOrNull { it.first.id == entry.id }?.second ?: 0

private fun importLocalPlaylist(context: Context, uri: Uri): PlayerSource {
    val folder = File(context.filesDir, "playlists").apply { mkdirs() }
    val id = UUID.randomUUID().toString()
    val destination = File(folder, "$id.m3u")
    context.contentResolver.openInputStream(uri)?.use { input ->
        destination.outputStream().use { output ->
            val buffer = ByteArray(16 * 1024)
            var size = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                size += count
                if (size > 512L * 1024 * 1024) throw IOException("Playlist exceeds the 512 MB import limit.")
                output.write(buffer, 0, count)
            }
        }
    } ?: throw IOException("Could not open the selected playlist.")
    return PlayerSource(
        id = id,
        name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Local playlist" } ?: "Local playlist",
        type = SourceType.LOCAL_M3U,
        serverUrl = destination.absolutePath,
    )
}

private fun downloadOnlineSubtitle(cacheDir: File, query: String, language: String, apiKey: String): File {
    fun request(url: String, method: String = "GET", body: String? = null): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 20000
            setRequestProperty("Api-Key", apiKey)
            setRequestProperty("User-Agent", "OATEC-PlayerTv v1.0")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("OpenSubtitles request failed (HTTP $code). Check the API key and quota.")
            return response
        } finally { connection.disconnect() }
    }
    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
    val lang = java.net.URLEncoder.encode(language.ifBlank { "en" }, "UTF-8")
    val search = JSONObject(request("https://api.opensubtitles.com/api/v1/subtitles?query=$encodedQuery&languages=$lang"))
    val data = search.optJSONArray("data") ?: throw IllegalStateException("No subtitles found for this title.")
    var fileId = 0
    var extension = "srt"
    for (i in 0 until data.length()) {
        val item = data.optJSONObject(i) ?: continue
        val attrs = item.optJSONObject("attributes") ?: continue
        val files = attrs.optJSONArray("files") ?: continue
        if (files.length() > 0) {
            val candidate = files.optJSONObject(0) ?: continue
            fileId = candidate.optInt("file_id", 0)
            extension = candidate.optString("file_name", "subtitle.srt").substringAfterLast('.', "srt").let { if (it.equals("vtt", true)) "vtt" else "srt" }
            if (fileId > 0) break
        }
    }
    if (fileId <= 0) throw IllegalStateException("No downloadable subtitle was found.")
    val download = JSONObject(request("https://api.opensubtitles.com/api/v1/download", "POST", JSONObject().put("file_id", fileId).toString()))
    val link = download.optString("link")
    if (link.isBlank()) throw IllegalStateException("OpenSubtitles did not return a download link.")
    val connection = (URL(link).openConnection() as HttpURLConnection).apply { connectTimeout = 12000; readTimeout = 30000 }
    try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("Subtitle download failed (HTTP ${connection.responseCode}).")
        val file = File(cacheDir, "online-subtitle-${System.currentTimeMillis()}.$extension")
        connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        if (file.length() == 0L) { file.delete(); throw IllegalStateException("Downloaded subtitle file is empty.") }
        return file
    } finally { connection.disconnect() }
}

@Composable
private fun KinoCheckTrailersScreen(
    onPlay: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val client = remember { KinoCheckClient() }
    var videos by remember { mutableStateOf<List<KinoCheckVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var refreshToken by remember { mutableStateOf(0) }

    LaunchedEffect(refreshToken) {
        loading = true
        error = ""
        runCatching {
            withContext(Dispatchers.IO) {
                (client.trendingTrailers(language = "en", limit = 30) +
                    client.latestTrailers(language = "en", limit = 30))
                    .distinctBy { it.youtubeVideoId }
                    .take(50)
            }
        }.onSuccess { videos = it }
            .onFailure { error = it.message ?: "تعذر تحميل التريلرات من KinoCheck." }
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(appText("Official Trailers", LocalAppLanguage.current), color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(appText("Latest and trending movie & series trailers", LocalAppLanguage.current), color = TextMuted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = { refreshToken++ }, enabled = !loading) {
                Text(if (loading) "Loading…" else "Refresh")
            }
        }
        when {
            loading && videos.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Lime)
            }
            error.isNotBlank() && videos.isEmpty() -> Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(error, color = ErrorRed, fontSize = 13.sp)
                OutlinedButton(onClick = { refreshToken++ }) { Text(appText("Try again", LocalAppLanguage.current)) }
            }
            videos.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(appText("ما كايناش تريلرات متاحة دابا.", LocalAppLanguage.current), color = TextMuted)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItems(videos, key = { it.youtubeVideoId }) { video ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onPlay(video.youtubeVideoId) },
                        color = Raised,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Column {
                            Box {
                                AsyncImage(
                                    model = video.thumbnailUrl.ifBlank { "https://img.youtube.com/vi/${video.youtubeVideoId}/hqdefault.jpg" },
                                    contentDescription = video.title,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                    contentScale = ContentScale.Crop,
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.Black.copy(alpha = 0.72f),
                                    shape = CircleShape,
                                ) {
                                    Text("▶", color = GoldLight, modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp), fontSize = 20.sp)
                                }
                            }
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(video.title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(video.categories.joinToString(" · ").ifBlank { "Official trailer" }, color = GoldLight, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
