import java.util.*

enum class Tipo {
    TERMINAL, NAO_TERMINAL, EPSILON
}

class Regra {
    var nome:String = ""
    var subRegras = mutableListOf<SubRegra>()
}

class SubRegra {
    var elementos = mutableListOf<Elemento>()
}

class Elemento {
    var nome: String = ""
    var tipo: Tipo = Tipo.EPSILON
}

val regras: MutableList<Regra> = mutableListOf()
val firstMap: HashMap<String, List<String>> = hashMapOf()
val followMap: HashMap<String, MutableList<String>> = hashMapOf()
val tabela: HashMap<Pair<Char, String>, SubRegra> = hashMapOf()

const val epsilon = "null"
const val endLineValue = "$"

fun main(args: Array<String>){
    val gramatica = """
        <expressao>::=<termo><expressao_linha>
        <expressao_linha>::=+<termo><expressao_linha>|-<termo><expressao_linha>|$epsilon
        <termo>::=<fator><termo_linha>
        <termo_linha>::=*<fator><termo_linha>|/<fator><termo_linha>|$epsilon
        <fator>::=(<expressao>)|<num>|<ide>
        <num>::=<dig><num_linha>
        <num_linha>::=<dig><num_linha>|$epsilon
        <ide>::=<letra><ide_linha>
        <ide_linha>::=<letra><ide_linha>|<dig><ide_linha>|$epsilon
        <dig>::=0|1|2|3|4|5|6|7|8|9
        <letra>::=A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z
    """.trimIndent()

    val string = "a90 * (35+9) / (b)$endLineValue".replace(" ".toRegex(), "")

    val tempoInicioGramatica = System.currentTimeMillis()
    processarGramatica(gramatica)
    val tempoFimGramatica = System.currentTimeMillis()

    val tempoInicioFirst = System.currentTimeMillis()
    gerarFirst()
    val tempoFimFirst = System.currentTimeMillis()

    val tempoInicioFollow = System.currentTimeMillis()
    gerarFollow()
    val tempoFimFollow = System.currentTimeMillis()

    val tempoInicioTabela = System.currentTimeMillis()
    gerarTabelaPreditiva()
    val tempoFimTabela = System.currentTimeMillis()

    val tempoInicioAnalise = System.currentTimeMillis()
    val stringValida = analiseSintatica(string)
    val tempoFimAnalise = System.currentTimeMillis()

    mostrarFirsts()
    mostrarFollows()

    println("String: $string -> Válida: $stringValida")
    println("Tempo para ler a gramática: ${tempoFimGramatica - tempoInicioGramatica} (ms)")
    println("Tempo para gerar o first: ${tempoFimFirst - tempoInicioFirst} (ms)")
    println("Tempo para gerar o follow: ${tempoFimFollow - tempoInicioFollow} (ms)")
    println("Tempo para gerar a tabela: ${tempoFimTabela - tempoInicioTabela} (ms)")
    println("Tempo para fazer a análise: ${tempoFimAnalise - tempoInicioAnalise} (ms)")
    println("Tempo execução total: ${tempoFimAnalise - tempoInicioGramatica} (ms)")
}

/**GRAMÁTICA**/

fun processarGramatica(texto:String){
    for(linha in texto.lines()){
        processarRegra(linha)?.let { regras.add(it) }
    }
}

fun processarRegra(texto:String): Regra? {
    val lista = texto.split("::=")
    return if(lista.size == 2){
        val regra = Regra()
        regra.nome = lista[0].replace("[<>]".toRegex(), "")
        regra.subRegras = processarSubRegra(lista[1])
        regra
    }else{
        null
    }

}

fun processarSubRegra(texto:String): MutableList<SubRegra>{
    val lista = texto.split("|")
    val subRegras = mutableListOf<SubRegra>()

    for(item in lista){
        val subRegra = SubRegra()
        subRegra.elementos = processarElementos(item)
        subRegras.add(subRegra)
    }

    return subRegras
}

fun processarElementos(texto:String): MutableList<Elemento>{

    val elementos = mutableListOf<Elemento>()

    var i = 0
    while(i < texto.length){
        val c = texto[i]

        val elemento = Elemento()
        if(c == '<'){
            val fechaPos = texto.indexOf('>', i)
            val nome = texto.substring(i+1, fechaPos)
            elemento.nome = nome
            elemento.tipo = Tipo.NAO_TERMINAL
            i = fechaPos
        }else if(c == 'n' && texto.length >= i+4 && texto.substring(i, i+4) == epsilon){
            elemento.nome = epsilon
            elemento.tipo = Tipo.EPSILON
            i+=4
        }else{
            var fimTerminal = texto.indexOf('<', i)
            if(fimTerminal == -1){
                fimTerminal = texto.length
            }

            elemento.nome = texto.substring(i, fimTerminal)
            elemento.tipo = Tipo.TERMINAL
            i = fimTerminal - 1
        }

        elementos.add(elemento)

        i++
    }

    return elementos
}

/**FIRST**/

fun gerarFirst(){
    for(regra in regras){
        gerarFirst(regra)
    }
}

fun gerarFirst(regra: Regra){
    var first = mutableListOf<String>()
    for(subRegra in regra.subRegras){
        first = first.union(gerarFirstSubRegra(subRegra, regra.nome)).toMutableList()
    }
    firstMap[regra.nome] = first
}

fun gerarFirstSubRegra(subRegra: SubRegra, nomeRegra: String): List<String>{
    var first = mutableListOf<String>()

    val primeiroElemento = subRegra.elementos[0]
    when(primeiroElemento.tipo){
        Tipo.TERMINAL -> {
            val chave = primeiroElemento.nome
            if(!firstMap.containsKey(chave)){
                firstMap[chave] = listOf(chave)
            }
            first.add(chave)
        }

        Tipo.EPSILON -> {
            firstMap[primeiroElemento.nome] = listOf(epsilon)
            first.add(epsilon)
        }

        Tipo.NAO_TERMINAL -> {
            for(elemento in subRegra.elementos){
                if(elemento.nome != nomeRegra){
                    if(!firstMap.containsKey(elemento.nome)){
                        gerarFirstElemento(elemento)
                    }
                    val firstElemento = firstMap[elemento.nome]
                    if(firstElemento != null){
                        if(first.contains(epsilon) && !firstElemento.contains(epsilon)){
                            first.remove(epsilon)
                        }
                        first = first.union(firstElemento).toMutableList()
                    }
                    if(first.isNotEmpty() && !first.contains(epsilon)){
                        break
                    }
                }
            }
        }
    }

    return first
}

fun gerarFirstElemento(elemento: Elemento){
    val chave = elemento.nome

    when(elemento.tipo){
        Tipo.TERMINAL -> {
            if(!firstMap.containsKey(chave)){
                firstMap[chave] = listOf(chave)
            }
        }

        Tipo.EPSILON -> {
            firstMap[chave] = listOf(epsilon)
        }

        Tipo.NAO_TERMINAL -> {
            regras.firstOrNull { it.nome == chave }?.let {
                gerarFirst(it)
            }
        }
    }
}

fun mostrarFirsts(){
    println("First:\n")
    for(chave in firstMap.keys){
        var first = ""
        var pular = false
        firstMap[chave]?.let {
            if(it.size > 1){
                for(string in it){
                    if(first.isEmpty()){
                        first = string
                    }else{
                        first += ", $string"
                    }
                }
            }else{
                pular = true
            }
        }

        if(!pular){
            println("$chave: $first")
        }
    }
    println()
}

/**FOLLOW**/

fun gerarFollow(){
    if(regras.isNotEmpty()){
        val regraInicial = regras[0]
        for(regra in regras){
            followMap[regra.nome] = mutableListOf()
        }
        //Regra 1 do Follow
        followMap[regraInicial.nome]?.add(endLineValue)
        followRegra2()
        followRegra3()
    }
}

fun followRegra2(){
    for(regra in regras){
        for(subRegra in regra.subRegras){
            val numElementos = subRegra.elementos.size
            if(numElementos > 2){
                for(i in 1 until subRegra.elementos.size-1){
                    val elemento = subRegra.elementos[i]
                    val proximo = subRegra.elementos[i+1]
                    if(elemento.tipo == Tipo.NAO_TERMINAL){
                        if(proximo.tipo == Tipo.NAO_TERMINAL){
                            firstMap[proximo.nome]?.let {  firstsProximo ->
                                followMap[elemento.nome]?.union(firstsProximo)?.toMutableList()?.let {
                                    it.remove(epsilon)
                                    followMap[elemento.nome] = it
                                }
                            }
                        }else if(proximo.tipo == Tipo.TERMINAL){
                            if(followMap[elemento.nome]?.contains(proximo.nome) == false){
                                followMap[elemento.nome]?.add(proximo.nome)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun followRegra3(){
    for(regra in regras){
        for(subRegra in regra.subRegras){
            val numElementos = subRegra.elementos.size
            for(i in 0 until numElementos){
                val elemento = subRegra.elementos[i]
                val proximoPos = i+1

                val checaProximo = checa@{
                    val proximo = subRegra.elementos[proximoPos]
                    if(proximo.tipo == Tipo.NAO_TERMINAL){
                        regras.firstOrNull{ it.nome == proximo.nome }?.let { r ->
                            for(sr in r.subRegras){
                                if(sr.elementos.firstOrNull{ it.tipo == Tipo.EPSILON } != null){
                                    return@checa true
                                }
                            }
                        }
                    }
                    return@checa false
                }

                if(elemento.tipo == Tipo.NAO_TERMINAL && (proximoPos == numElementos || checaProximo())){
                    followMap[regra.nome]?.let { followRegra ->
                        followMap[elemento.nome]?.union(followRegra)?.toMutableList()?.let {
                            followMap[elemento.nome] = it
                        }
                    }
                }
            }
        }
    }
}

fun mostrarFollows(){
    println("Follow:\n")
    for(chave in followMap.keys){
        var follow = ""
        followMap[chave]?.let { lista ->
            for(string in lista){
                if(follow.isEmpty()){
                    follow = string
                }else{
                    follow += ", $string"
                }
            }
        }

        println("$chave: $follow")
    }
    println()
}

/**TABELA PREDITIVA**/

fun gerarTabelaPreditiva(){
    for(regra in regras){
        for(subRegra in regra.subRegras){
            if(subRegra.elementos.size > 0){
                val primeiroElemento = subRegra.elementos[0]

                when(primeiroElemento.tipo){
                    Tipo.TERMINAL -> {
                        val char = primeiroElemento.nome.toCharArray()[0]
                        tabela[Pair(char, regra.nome)] = subRegra
                    }
                    Tipo.NAO_TERMINAL -> {
                        firstMap[primeiroElemento.nome]?.let { first ->
                            for(terminal in first){
                                val char = terminal.toCharArray()[0]
                                tabela[Pair(char, regra.nome)] = subRegra
                            }
                        }
                    }
                    Tipo.EPSILON -> {
                        followMap[regra.nome]?.let { follow ->
                            for(terminal in follow){
                                val char = terminal.toCharArray()[0]
                                tabela[Pair(char, regra.nome)] = subRegra
                            }
                        }
                    }
                }
            }
        }
    }
}

fun analiseSintatica(string: String) : Boolean {
    if(regras.isNotEmpty()){
        val pilha = Stack<Elemento>()

        val inicio = Elemento()
        inicio.nome = regras[0].nome
        inicio.tipo = Tipo.NAO_TERMINAL

        val fim = Elemento()
        fim.nome = endLineValue
        fim.tipo = Tipo.TERMINAL

        pilha.push(fim)
        pilha.push(inicio)

        var cursor = 0
        while(pilha.size > 0){
            if(cursor > string.length){
                return false
            }

            val char = string[cursor]
            val topo = pilha.peek()
            if(topo != null){
                when(topo.tipo){
                    Tipo.TERMINAL -> {
                        if(char == topo.nome.toCharArray()[0]){
                            pilha.pop()
                            cursor++
                        }else{
                            return false
                        }
                    }

                    Tipo.NAO_TERMINAL -> {
                        val subRegra = tabela[Pair(char, topo.nome)]
                        if(subRegra != null){
                            pilha.pop()
                            for(elemento in subRegra.elementos.reversed()){
                                pilha.push(elemento)
                            }
                        }else{
                            return false
                        }
                    }

                    Tipo.EPSILON -> {
                        pilha.pop()
                    }
                }
            }else{
                return false
            }
        }

        return true
    }

    return false
}