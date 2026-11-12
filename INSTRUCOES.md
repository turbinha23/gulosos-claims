# GulososClaims — Guia de instalação (Paper 1.21.11)

Sistema de terrenos do GulososSMP: `/claim`, `/terrenos`, `/blocos`, com o logo **GulososSMP**
nas mensagens do chat, nos títulos e no menu, usando a tua textura.

---

## 1. O que vem no pacote

| Ficheiro | Para quê |
|---|---|
| `GulososClaims-projeto.zip` | Código-fonte do plugin (Java + Maven) |
| `GulososSMP-ResourcePack.zip` | A tua textura + o logo horizontal do chat (`\ue00b`) e `pack_format` da 1.21.11 |

Glifos do resource pack usados pelo plugin:

| Glifo | Imagem | Onde aparece |
|---|---|---|
| `\ue00b` | `gulosos_chat.png` (novo, horizontal) | Prefixo de **todas as mensagens** do plugin, título dos menus, holograma 3D |
| `\ue008` | `gulosos_24.png` | **Títulos** no ecrã (boas-vindas, entrar num terreno, terreno criado, contagem do teleporte) |
| `\ue000`–`\ue006` | tags (founder, admin, mod, helper, builder, vip, membro) | Antes do nome no chat |

---

## 2. Compilar o plugin (gerar o .jar)

> ⚠️ O código foi escrito para a API Paper 1.21.11, mas **não foi compilado nem testado em servidor real**
> no ambiente onde o criei. Se o compilador apontar algum erro, copia a mensagem e envia-me que corrijo.

### Opção A — GitHub (não instalas nada)
1. Cria um repositório no GitHub e envia o conteúdo da pasta `GulososClaims` (descompactada).
2. Separador **Actions → Compilar plugin → Run workflow**.
3. No fim, descarrega o artefacto **GulososClaims** → contém `GulososClaims.jar`.

### Opção B — No teu PC
1. Instala o **JDK 21** (Temurin) e o **Maven**.
2. Na pasta do projeto: `mvn clean package`
3. O ficheiro sai em `target/GulososClaims.jar`.

### Opção C — IntelliJ IDEA
Abre a pasta como projeto Maven → painel Maven → `Lifecycle → package`.

---

## 3. Resource pack (obrigatório para ver o logo)

Sem o pack, os glifos aparecem como quadrados.

1. Faz upload do `GulososSMP-ResourcePack.zip` para um sítio com **link direto de download**
   (ex.: GitHub Releases, o teu host, ou um serviço de packs). O link tem de descarregar o ficheiro, não abrir uma página.
2. No `server.properties`:
```properties
resource-pack=https://O-TEU-LINK-DIRETO/GulososSMP-ResourcePack.zip
resource-pack-sha1=460ac9d403d19049c4fc0b0e2b1ac9e4255c7c9f
require-resource-pack=true
resource-pack-prompt=Textura oficial do GulososSMP
```
3. Se alterares o zip, tens de recalcular o SHA-1 (`sha1sum ficheiro.zip` no Linux, ou
   `certutil -hashfile ficheiro.zip SHA1` no Windows) e atualizar o `resource-pack-sha1`.
   O valor acima é do zip que te entreguei.
4. Se o jogo avisar que o pack é de outra versão, confirma em `pack.mcmeta` o `pack_format`
   (está a **75**, que corresponde à 1.21.11). É só um aviso, o pack carrega na mesma.

---

## 4. Instalar no servidor

1. **Remove conflitos** da configuração anterior:
   - Apaga o **GriefPrevention** (e o DeluxeMenus só se for usado apenas para o `/terrenos`).
   - No `commands.yml` apaga os aliases `claim:` e `blocos:` que tinhas feito.
   - Apaga o kit `claimtool` do Essentials, se existir.
2. Coloca `GulososClaims.jar` em `plugins/`.
3. Reinicia o servidor (não uses `/reload`).
4. Entra no jogo e testa: `/claim`, `/blocos`, `/terrenos`.

O plugin cria `plugins/GulososClaims/config.yml` e `dados.yml` (terrenos e blocos; **faz backup deste ficheiro**).

---

## 5. Como se usa

| Comando | O que faz |
|---|---|
| `/claim` | Dá a **Pá de Terrenos** (pá dourada especial) |
| `/claim confirmar` | Cria o terreno com os 2 cantos marcados (também há botão clicável no chat) |
| `/claim cancelar` | Cancela a seleção |
| `/blocos` | Mostra blocos totais, usados e restantes |
| `/terrenos` | Abre o menu 3D de terrenos |
| `/blocos dar <jogador> <n>` | **Admin:** dá blocos extra (`gulosos.admin`) |

**Criar um terreno:** `/claim` → botão direito num canto → botão direito no canto oposto →
o chat mostra o tamanho e o custo, as partículas desenham o limite → **[CONFIRMAR]**.
O terreno vai do fundo ao topo do mundo e os blocos usados são descontados.

**Menu `/terrenos`:** blocos, receber pá, lista de terrenos (com páginas), terreno onde estás, ver limites,
ajuda. Dentro de cada terreno: **teleportar** (contagem de 3s com título), **renomear**, **membros**
(adicionar/remover), **ver limites**, interruptores de **PvP / explosões / fogo / mobs destrutivos** e **abandonar**
(devolve os blocos).

**Efeitos:** moldura do menu animada, títulos com o logo e texto letra a letra, pilares de partículas nos cantos,
limites desenhados com partículas e, ao criar um terreno, um **selo 3D** (logo flutuante + pá a rodar)
por cima do terreno durante ~7 segundos.

---

## 6. Tags no chat (LuckPerms)

O plugin mostra a tag segundo a permissão (a primeira que a pessoa tiver, por esta ordem):
`gulosos.tag.founder` › `admin` › `mod` › `helper` › `builder` › `vip`. Sem nenhuma aparece **MEMBRO**.

```
/lp group founder permission set gulosos.tag.founder true
/lp group admin   permission set gulosos.tag.admin true
/lp group mod     permission set gulosos.tag.mod true
/lp group helper  permission set gulosos.tag.helper true
/lp group builder permission set gulosos.tag.builder true
/lp group vip     permission set gulosos.tag.vip true
```
Para desligar as tags no chat: `chat.tags: false` no `config.yml`.
Se já usas um plugin de chat (EssentialsChat, LuckPerms-chat…), pode sobrepor-se a este formato; desliga um dos dois.

---

## 7. Configuração (`config.yml`)

```yaml
blocos:
  iniciais: 200         # blocos de início
  por-hora: 100         # ganhos por hora online
terrenos:
  tamanho-minimo: 5
  maximo-por-jogador: 10
  mundos-bloqueados: [] # ex: [world_the_end]
titulos:
  entrar-terreno: true
  boas-vindas: true
efeitos:
  selo-3d: true
```
Depois de editar: reinicia o servidor.

---

## 8. Proteção incluída

Quem não é dono nem membro **não pode**: partir/colocar blocos, abrir baús/portas/botões, usar baldes,
colocar barcos/armor stands, mexer em molduras, ferir animais/aldeões, nem pisar plantações.
Por terreno há ainda: PvP, explosões, propagação de fogo e mobs que mexem em blocos (tudo **desativado** por defeito),
e a água/lava de fora não entra. Admins com `gulosos.admin` ignoram a proteção.

## 9. Limitações honestas

- **3D realista:** a textura que enviaste tem só o logo e as tags (2D). O "3D" que o plugin faz é com entidades
  de display (selo flutuante, pá a rodar, partículas). Fundos de menu desenhados ou modelos 3D (Blockbench) não existem
  no pack; se os criares, dá para ligar ao menu (fonte com fundo no título e `item_model` nos botões).
- Sem economia: os blocos vêm de tempo de jogo e do `/blocos dar`. Comprar blocos com dinheiro exige o Vault.
- Não há redimensionar terreno (apaga e cria de novo) nem sub-terrenos.
- Os terrenos não protegem contra pistões vindos de fora em todos os casos, nem contra plugins que mexam em blocos sem eventos.
