# Media Tracker

Aplicativo Android para acompanhar o que estou lendo e assistindo: **livros, filmes e séries**.
Cada item tem progresso (páginas, minutos ou episódios), nota e anotações. Os livros podem ser
buscados na [Open Library](https://openlibrary.org/dev/docs/api/search) e salvos na biblioteca pessoal.

Trabalho individual da disciplina AC322A (Programação Mobile), UNAERP.

> **Autor:** Felipe Martins Nascimento - RA 842399

## Objetivo

Resolver um problema simples: saber em que ponto parei em cada livro, filme ou série.
O fluxo principal é: **buscar um livro → adicionar à biblioteca → abrir o detalhe → atualizar o progresso e a nota →
fechar o app e abrir de novo e continuar de onde parou** (os dados ficam salvos no aparelho).

## Como rodar

**Requisitos**

- Android Studio atual (o projeto usa AGP 9.3.1 e Kotlin 2.2.10 — as mesmas versões do projeto-base da disciplina).
- JDK 25 (o Android Studio já traz o `jbr-25`; o `gradle-daemon-jvm.properties` pede o toolchain 25).
- Aparelho ou emulador com **Android 13 (API 33) ou superior** (`minSdk = 33`).
- Internet na primeira sincronização do Gradle e para usar a busca de livros.

**Passos**

1. Clone o repositório e abra a pasta no Android Studio.
2. Aguarde o _Gradle Sync_ terminar.
3. Escolha um emulador/aparelho e rode a configuração `app`.

Pela linha de comando: `./gradlew assembleDebug` (ou `./gradlew installDebug` com um aparelho conectado).

**Testes unitários:** `./gradlew testDebugUnitTest`

**Verificação automática:** o arquivo `.github/workflows/build.yml` faz o GitHub compilar o projeto e rodar os
testes a cada push (aba *Actions* do repositório). Um ✔ verde significa que ele compila numa máquina limpa.
O APK de debug fica disponível como artefato da execução.

**Chaves e senhas:** o app **não usa nenhuma**. A Open Library é pública e não pede cadastro, então não há
arquivo `.env` para enviar. O `.gitignore` também bloqueia `.env`, keystores e `secrets.properties` por precaução.

## Onde está cada requisito da avaliação

### Parcial — Views XML, navegação e Intent

| Requisito | Onde |
|---|---|
| Duas telas em XML | `res/layout/activity_views_list.xml`, `activity_views_detail.xml` |
| Views e ViewGroups | `LinearLayout`, `FrameLayout`, `ScrollView`, `RecyclerView`, `TextView`, `ImageView` |
| Navegação por `Intent` explícita com dados | `ViewsListActivity.openDetail` envia `EXTRA_BOOK_ID` para `ViewsDetailActivity` |
| ViewBinding e interação que atualiza a UI | botão "Li mais 10 páginas" em `ViewsDetailActivity` |
| `data class` imutável e opcionais | `ViewsBook` (`author`, `synopsis` e `year` são `null`áveis) |
| Dados simulados | `ViewsMockData` |
| Opcional: componente XML reutilizável | `item_views_book.xml`, inflado pelo adapter |

Para abrir essas telas: no app, menu `⋮` da biblioteca → **Telas em Views (XML)**.

### Etapa 2 — Jetpack Compose

| Requisito | Onde |
|---|---|
| Telas coerentes com o tema | biblioteca, busca, detalhe e formulário (`ui/*`) |
| Acessibilidade | `contentDescription` em ícones e botões, áreas de toque de 48dp (padrão do Material 3), tipografia em `sp`, telas com rolagem para fontes grandes, cores do tema Material (contraste), `liveRegion` no erro |
| Navigation 3 | `ui/navigation/AppNavigation.kt` (rotas `NavKey`, `NavDisplay`, back stack salvo, argumentos em `DetailKey`/`FormKey`) |
| `LazyColumn` / `LazyRow` | resultados da busca (`LazyColumn`), filtros (`LazyRow`) e biblioteca (`LazyVerticalGrid`) |
| Formulário com validação | `ui/form/MediaFormScreen.kt` + `domain/MediaFormValidator.kt` |
| `ViewModel`, `UiState`/`StateFlow`, `collectAsStateWithLifecycle`, fluxo unidirecional | todos os `*ViewModel` e `*Route` |
| Estados de carregamento, conteúdo, vazio e erro | `ui/components/StateViews.kt` e os `UiState` de cada tela |
| Estado após rotação | ViewModels por entrada de navegação + `rememberSaveable` (texto da busca, diálogo) + back stack serializável |
| `Repository` | `data/MediaRepository.kt` |
| Operação assíncrona com coroutines e cancelamento | busca em `SearchViewModel` (nova busca cancela a anterior; `runInterruptible` interrompe a requisição) |
| Persistência local com exposição reativa | `data/local/DataStoreLibraryDataSource.kt` (DataStore, devolve `Flow`) |
| Sem segredos no GitHub | nenhum segredo existe no projeto |

**Opcionais implementados**

- DTO → modelo de domínio (`data/remote/BookDto.kt`).
- Testes automatizados (regra de negócio, validação, repositório com fakes e ViewModels) em `app/src/test`.
- Cache da API com política de expiração (TTL de 5 min, em memória) em `DefaultMediaRepository`.
- Funciona offline para o que já foi salvo (a biblioteca é local; só a busca precisa de internet).
- Animações implícitas (`animateFloatAsState`, `animateContentSize`).
- Layout adaptável (`GridCells.Adaptive`, largura máxima em telas largas).
- Injeção manual de dependências (`AppContainer` + factories em `ui/ViewModelFactories.kt`), interfaces e fakes nos testes.
- Integração com o sistema: compartilhar o progresso (`Intent.ACTION_SEND`).
- Boas práticas: entradas validadas, nenhum dado do usuário em logs.

## Arquitetura

```
ui (Compose)  →  ViewModel  →  Repository (interface)  →  fontes de dados
 telas "burras"   UiState        DefaultMediaRepository     local: DataStore (Flow)
 recebem estado   StateFlow      cache TTL da busca         remota: Open Library (HttpURLConnection)
 e emitem ações
```

- **domain/**: modelos e regras puras, sem Android (por isso são testáveis na JVM).
  O status (`PLANNED/IN_PROGRESS/COMPLETED`) é **calculado** a partir do progresso, nunca gravado,
  para não existir estado inconsistente.
- **data/**: `MediaRepository` é a única porta de dados da UI. As fontes (`LibraryDataSource`,
  `BookRemoteDataSource`) são interfaces, então os testes usam fakes em memória.
- **ui/**: cada tela tem um `*Route` (liga ao ViewModel) e um `*Screen` (só recebe estado e callbacks, fácil de
  pré-visualizar e testar). Eventos de uso único (voltar após excluir/salvar) usam `Channel`, para não
  repetir após rotação.
- **Navegação**: cada tela da pilha tem seu próprio ViewModel, descartado ao sair dela.

### Decisões que preciso saber explicar

- **DataStore em vez de Room.** A biblioteca é uma lista pequena, sem consultas relacionais. Guardar a lista em
  JSON num DataStore evita esquema e migrações. Se a lista crescesse muito, Room seria melhor.
- **`HttpURLConnection` + kotlinx.serialization em vez de Retrofit.** Só existe uma chamada GET. Menos dependências,
  e `runInterruptible` garante o cancelamento da requisição.
- **Total de páginas pode ser 0.** A API nem sempre informa páginas; nesse caso o detalhe pede para o usuário informar
  o total, em vez de inventar um valor.
- **Progresso do slider só é gravado ao soltar.** Evita centenas de escritas no disco durante o arraste.
- **Open Library em vez de TMDB/Google Books.** Não exige chave de API, então o projeto roda para qualquer pessoa
  sem arquivo `.env` — importante porque o professor precisa compilar e executar sem alterações.

## Bibliotecas externas

| Biblioteca | Para que serve |
|---|---|
| Jetpack Compose (BOM) + Material 3 | interface declarativa das telas da Etapa 2 |
| Navigation 3 (`navigation3-runtime`, `navigation3-ui`) | navegação entre telas, com back stack e argumentos |
| Lifecycle (`viewmodel-compose`, `runtime-compose`, `viewmodel-navigation3`) | `ViewModel`, `collectAsStateWithLifecycle` e ViewModels por entrada de navegação |
| DataStore Preferences | persistência local da biblioteca |
| kotlinx.serialization (JSON) | ler a resposta da API e gravar a biblioteca |
| kotlinx.coroutines | operações assíncronas e `Flow` |
| Coil 3 (`coil-compose`, `coil-network-okhttp`) | carregar as capas dos livros pela internet |
| Material Components, AppCompat, ConstraintLayout/RecyclerView (transitivo) | telas em Views XML da Parcial |
| JUnit, kotlinx-coroutines-test | testes unitários |

## Estrutura de pastas

```
app/src/main/java/com/felipe/mediatracker/
├── domain/        modelos e regras (MediaItem, MediaFormValidator)
├── data/          Repository, fonte local (DataStore) e remota (Open Library)
├── ui/            Compose: library, search, detail, form, components, navigation, theme
├── views/         Parcial em Views XML (duas Activities + mocks)
├── AppContainer.kt  injeção manual de dependências
└── MainActivity.kt
```

## Fonte dos dados

Livros e capas: [Open Library](https://openlibrary.org) (API aberta, sem chave).
