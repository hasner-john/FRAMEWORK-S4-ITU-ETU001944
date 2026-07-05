Reprenons calmement, sans code cette fois — juste pour que le concept soit clair.
## Le problème que sprint 3 résout
Jusqu'au sprint 2, ta map de routes ressemblait à ça :
"/hello" -> HelloController.sayHello()
La clé, c'était juste l'URL en String. Problème : dans une vraie appli web, on veut souvent la même URL mais un comportement différent selon le verbe HTTP. Exemple classique : un formulaire d'inscription.

- GET /inscription → affiche le formulaire vide
- POST /inscription → traite les données envoyées et crée l'utilisateur

Avec une clé String simple, c'est impossible : la deuxième méthode annotée @UrlMapping("/inscription") écraserait silencieusement la première dans ta Map, ou créerait une ambiguïté.

## La solution : une clé composite
Au lieu de Map<String, RouteMapping>, on passe à Map<UrlMethode, RouteMapping>, où UrlMethode regroupe deux infos : l'url ET le verbe HTTP (GET ou POST). C'est un objet simple, comme une petite "carte d'identité" de route.
Pourquoi a-t-on besoin de redéfinir equals/hashCode dessus ? Parce qu'en Java, par défaut, deux objets new UrlMethode("/x", GET) créés séparément sont considérés différents (comparaison par référence mémoire), même s'ils contiennent exactement les mêmes valeurs. Or une HashMap utilise equals/hashCode pour savoir si une clé existe déjà. Sans cette redéfinition, ta map ne détecterait jamais de doublon, même avec deux fois exactement le même couple url+méthode.
En redéfinissant equals pour dire "deux UrlMethode sont égaux si même url ET même méthode HTTP", tu obtiens :

- ("/hello", GET) et ("/hello", GET) → considérés identiques → conflit détecté
- ("/hello", GET) et ("/hello", POST) → considérés différents → coexistence normale

## Le test demandé par le sprint
L'énoncé dit : si deux méthodes ont le même url et le même verbe HTTP, ça doit lever une exception au démarrage. C'est une protection contre une erreur de programmation : si un développeur, par erreur, met deux @UrlMapping("/x") en GET dans deux méthodes différentes, le framework doit le signaler clairement plutôt que de laisser l'une écraser l'autre silencieusement.
Niveau code, concrètement

- HttpMethod — un enum à deux valeurs, GET et POST, pour représenter le verbe HTTP de façon typée (plus sûr qu'une String libre où on pourrait écrire "Get", "get", "GET " par erreur).
UrlMethode — la classe clé : deux champs (url, methode), avec equals et hashCode qui se basent sur ces deux champs ensemble.
- UrlMapping (l'annotation) — on lui ajoute un attribut methode() avec une valeur par défaut GET, pour que les méthodes existantes du sprint 2 (qui n'ont pas encore précisé de verbe) continuent de fonctionner sans modification.
- FrontControllerServlet.scanRoutes() — au lieu de juste faire map.put(url, route), on vérifie d'abord if (map.containsKey(clé)) avant d'insérer. Si la clé existe déjà, on lève l'exception plutôt que d'écraser.
- doGet/doPost — avant, une seule méthode processRequest recevait juste l'url. Maintenant elle reçoit aussi le verbe HTTP (GET ou POST selon que la requête est arrivée via doGet ou doPost), pour construire la bonne clé UrlMethode et chercher la bonne route.