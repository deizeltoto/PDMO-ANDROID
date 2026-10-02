# Servidor PDMO

API Express, SQLite persistente e painel HTML/CSS/JavaScript para gerir os dados da aplicação Android. Requer **Node.js 24 ou superior**; SQLite usa o módulo integrado `node:sqlite`, sem instalar outro servidor de base de dados.

## Iniciar

No PowerShell, dentro da pasta `backend`:

```powershell
npm ci
Copy-Item .env.example .env  # Apenas se ainda não existir .env
# Edite .env e defina ADMIN_TOKEN com uma chave longa.
npm start
```

Nesta implementação foi criado um `.env` local com uma chave aleatória. Para entrar no painel, copie o valor de `ADMIN_TOKEN` desse ficheiro. A chave não é incluída no Git, não é necessária no Android e fica apenas na memória da página durante a sessão. Recarregar a página termina a sessão.

- Painel: <http://localhost:3000>
- Estado: <http://localhost:3000/api/health>
- Catálogo Android: <http://localhost:3000/api/sync>
- Testes: `npm test`
- Desenvolvimento: `npm run dev`

Variáveis: `HOST` (predefinido `0.0.0.0`), `PORT` (3000), `ADMIN_TOKEN`, `DB_PATH` (caminho relativo a `backend` ou absoluto). Sem chave, a consulta funciona e a escrita fica desactivada. Reinicie o servidor depois de alterar `.env`.

## Comunicar com a aplicação

### Cânticos com áudio e letra

No painel, abra **Cânticos → Editar/Novo**, escreva a letra e preencha **Link directo do áudio** com o endereço HTTP/HTTPS de um ficheiro MP3, M4A, OGG ou WAV. Pode ouvir uma pré-escuta no próprio formulário. Guarde e sincronize a aplicação. O endereço deve ser acessível pelo telemóvel (não use `localhost` para apontar ao computador). Em produção use HTTPS. Links para páginas do YouTube/Spotify não são links directos de áudio.

No detalhe do cântico, a aplicação mostra o leitor acima da letra: reproduzir/pausar, progresso, avanço e duração. O leitor permanece disponível ao percorrer a letra e pausa ao sair da página, passar a aplicação para segundo plano, perder foco de áudio ou desligar auscultadores. Erros de ligação/formato mostram uma mensagem e permitem tentar novamente.

`audioUrl` é opcional. Sem áudio, a letra continua a funcionar. Para remover o áudio, limpe o campo no painel e guarde. A letra fica disponível offline; **o áudio é transmitido pela rede e não é descarregado para uso offline**. Esta versão usa links para ficheiros já alojados; não inclui upload de áudio nem letra sincronizada por tempo.

O servidor acrescenta a coluna `songs.audioUrl` automaticamente nas bases existentes. A migração Room **5 → 6** acrescenta a mesma coluna sem apagar dados ou favoritos. Servidores antigos sem o campo continuam compatíveis; aplicações antigas ignoram o novo campo.

1. Compile/instale a versão **debug** actualizada da aplicação.
2. Abra **Servidor**, no topo da aplicação.
3. No emulador Android padrão, use `http://10.0.2.2:3000`.
4. Num telemóvel, use `http://IP-DO-COMPUTADOR:3000`, na mesma rede Wi-Fi. Consulte o IPv4 do computador com `ipconfig`; a firewall deve permitir a porta configurada na rede privada.
5. Toque em **Sincronizar agora**. O resultado e a última sincronização aparecem no ecrã.
6. Crie ou edite um registo no painel e volte a sincronizar para o ver na aplicação.

O endereço é a raiz do servidor, sem `/api`. A sincronização é manual: não existe push nem actualização automática em segundo plano. A versão release requer HTTPS; a permissão de HTTP está limitada ao manifesto debug. Para usar fora de uma rede de desenvolvimento, configure HTTPS num proxy à frente do servidor.

## Análise do projecto existente

- Android em Kotlin, Jetpack Compose, ViewModels, repositórios e DAOs Room com `Flow`.
- Base original `app_pdmo_database`, versão 4, com oito tabelas: conteúdos, mensagens, cânticos, livros, versículos e três tabelas de favoritos.
- Os dados provinham de `app/src/main/assets/{contents,songs,bible}.json` e de exemplos no código. Não existiam chamadas HTTP nem permissão de internet.
- O backend tinha Express 5 instalado, `index.js` vazio e nenhum banco, API ou interface.
- O ficheiro de conteúdos usa datas textuais em português; o Android espera milissegundos. A importação converte essas datas para timestamps UTC.
- Os assets contêm 9 conteúdos, 8 cânticos, 13 livros e 70 versículos. **Não são uma Bíblia completa.** O total de capítulos indicado num livro não significa que todos os capítulos tenham texto.
- Favoritos e última leitura são dados pessoais locais; não há contas de utilizador ou sincronização desses dados entre dispositivos.

## Arquitectura em camadas

```text
public/                        Painel de gestão
index.js                       Arranque, configuração e encerramento
src/app.js                     Composição Express e tratamento de erros
src/routes/                    Endpoints e autenticação de escrita
src/controllers/               Adaptação HTTP: pedidos e respostas
src/services/                  Validação e regras de negócio
src/repositories/              Acesso SQL e operações de catálogo
src/domain/                    Definições de recursos e erros
src/database/                  Esquema, ligação e importação inicial
src/middleware/                Autenticação por chave Bearer
test/                          Testes de integração HTTP com SQLite temporário
data/pdmo.sqlite               Banco gerado localmente (ignorado pelo Git)
```

## Estrutura do banco do servidor

| Tabela | Campos principais | Regras |
| --- | --- | --- |
| `contents` | id, title, description, body, author, type, imageUrl, createdAt | Tipo: `ESTUDO`, `Pregação` ou `ARTIGO`; timestamp em ms |
| `songs` | id, number, title, category, lyrics, author, audioUrl | Número positivo e único; áudio HTTP/HTTPS opcional |
| `daily_messages` | id, message, bibleReference, date | Data `AAAA-MM-DD`; a aplicação mostra a mensagem com maior ID |
| `bible_books` | id, name, abbreviation, testament, bookOrder, chapterCount | Ordem única; testamento `OLD_TESTAMENT` ou `NEW_TESTAMENT` |
| `bible_verses` | id, bookId, chapter, verse, text | FK para livro; referência única `(bookId, chapter, verse)` |
| `metadata` | key, value | Importação concluída e revisão do catálogo |

IDs são gerados pelo servidor e nunca reutilizados. Um livro com versículos não pode ser eliminado; remova os versículos primeiro. A referência de um versículo existente é imutável para preservar a identidade dos favoritos. É possível alterar o texto. O serviço impede capítulos acima do total do livro.

Na primeira abertura, o servidor importa os assets numa transacção. Nas seguintes, conserva o banco, mesmo se o administrador tiver eliminado todos os registos de uma tabela. O projecto Android deve estar presente na importação inicial. Para salvaguardar os dados, pare o servidor antes de copiar `data/`; SQLite usa WAL enquanto está em execução.

## API

| Método | Caminho | Função |
| --- | --- | --- |
| GET | `/api/health` | Estado do servidor |
| POST | `/api/admin/session` | Verificar a chave administrativa |
| GET | `/api/sync` | Catálogo completo, `schemaVersion: 1`, revisão e data de geração |
| GET | `/api/:resource` | Lista; filtro `q` para pesquisa |
| GET | `/api/:resource/:id` | Consultar um registo |
| POST | `/api/:resource` | Criar um registo |
| PUT | `/api/:resource/:id` | Substituir os campos editáveis |
| DELETE | `/api/:resource/:id` | Eliminar um registo |

Recursos: `contents`, `songs`, `daily-messages`, `books`, `verses`. As listas também aceitam filtros exactos `type`, `category`, `testament`, `bookId` e `chapter`, quando aplicáveis. Exemplo: `/api/verses?bookId=1&chapter=1`.

Escrita e verificação de sessão exigem `Authorization: Bearer VALOR_DE_ADMIN_TOKEN`. Leitura é pública, como o catálogo da aplicação. PUT requer todos os campos obrigatórios; `createdAt` é preservado se omitido. Não envie `id` no corpo. Erros têm formato `{ "error": "mensagem" }` e códigos 400, 401, 404, 409, 413 ou 503 conforme o caso.

Exemplo de corpo para criar um conteúdo:

```json
{
  "title": "Encontro da comunidade",
  "description": "Reflexão da semana",
  "body": "Texto completo da reflexão.",
  "author": "Equipa pastoral",
  "type": "ARTIGO",
  "imageUrl": null
}
```

## Sincronização e funcionamento offline

`CatalogSyncRepository` descarrega e valida o catálogo antes de alterar Room. Uma transacção substitui as cinco tabelas de publicação e regista o servidor/data na nova tabela `sync_state`. Se o pedido ou a transacção falhar, o catálogo anterior permanece disponível. Limite de resposta: 32 MB; adequado ao catálogo actual. Catálogos maiores deverão usar paginação/sincronização incremental.

A migração Room **4 → 5** adiciona apenas `sync_state`, preservando os dados existentes. O comportamento preexistente de migração destrutiva para versões anteriores à 4 mantém-se. A carga de exemplos e a sincronização partilham um mutex; após uma sincronização, os exemplos deixam de ser inseridos, inclusive quando o catálogo remoto está vazio.

Favoritos de conteúdos e cânticos são conservados pelos IDs. Os favoritos bíblicos são remapeados pela referência `(livro, capítulo, versículo)`. Favoritos de registos eliminados são removidos. A última leitura continua nas preferências locais. Use uma única base de servidor para manter a identidade do catálogo; trocar para uma base independente com IDs diferentes pode alterar a correspondência dos favoritos.

As notificações existentes continuam locais; este servidor não implementa Firebase/push, carregamento de ficheiros, contas individuais ou agendamento da mensagem do dia.

## Validação

`npm test` executa dez testes de integração cobrindo o contrato Android, áudio, migração do banco, interface estática, CRUD, pesquisa, autenticação, validação, referências bíblicas, IDs estáveis, revisão e persistência ao reabrir SQLite. Cada teste usa uma base temporária isolada.

Validação desta implementação: **10 testes da API passaram**, **5 testes Android passaram no emulador** e **APK debug compilado com sucesso**. `CatalogSyncTest` usa Room em memória e um servidor HTTP temporário para verificar substituição de dados/favoritos, catálogo vazio sem reinserir exemplos, áudio com letra e rollback após falha. `SongAudioTest` gera um WAV silencioso temporário e valida carregar, reproduzir, pausar, avançar e libertar o leitor. Os testes de sincronização não usam os dados reais do dispositivo.

Neste ambiente Windows, o Java apresentou `Unable to establish loopback connection`. A compilação foi concluída com o Java do Android Studio e a opção de processo `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\pdmo-unused-socket-dir`, usando o fallback TCP do Java. Essa opção não foi gravada nas configurações do projecto ou do sistema.

Para verificar Android: `./gradlew.bat :app:assembleDebug`. Depois valide num emulador/dispositivo: sincronizar, marcar favoritos, alterar/eliminar registos no painel, voltar a sincronizar e abrir a aplicação sem rede. Compilar o APK não substitui esse teste num dispositivo.
