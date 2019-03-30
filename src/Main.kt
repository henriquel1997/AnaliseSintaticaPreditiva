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

const val nullValue = "null"
const val endLineValue = "$"

fun main(){
    val gramatica = """
        <expressao>::=<termo><expressao_linha>
        <expressao_linha>::=+<termo><expressao_linha>|-<termo><expressao_linha>|$nullValue
        <termo>::=<fator><termo_linha>
        <termo_linha>::=*<fator><termo_linha>|/<fator><termo_linha>|$nullValue
        <fator>::=(<expressao>)|<num>|<ide>
        <num>::=<dig><num_linha>
        <num_linha>::=<dig><num_linha>|$nullValue
        <ide>::=<letra><ide_linha>
        <ide_linha>::=<letra><ide_linha>|<dig><ide_linha>|$nullValue
        <dig>::=0|1|2|3|4|5|6|7|8|9
        <letra>::=A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z
    """.trimIndent()

    //<letra>::=A|B|C

    processarGramatica(gramatica)
    gerarFirst()
    mostrarFirsts()
    gerarFollow()
    mostrarFollows()
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
        }else if(c == 'n' && texto.length >= i+4 && texto.substring(i, i+4) == nullValue){
            elemento.nome = nullValue
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
            firstMap[primeiroElemento.nome] = listOf(nullValue)
            first.add(nullValue)
        }

        Tipo.NAO_TERMINAL -> {
            for(elemento in subRegra.elementos){
                if(elemento.nome != nomeRegra){
                    if(!firstMap.containsKey(elemento.nome)){
                        gerarFirstElemento(elemento)
                    }
                    val firstElemento = firstMap[elemento.nome]
                    if(firstElemento != null){
                        if(first.contains(nullValue) && !firstElemento.contains(nullValue)){
                            first.remove(nullValue)
                        }
                        first = first.union(firstElemento).toMutableList()
                    }
                    if(first.isNotEmpty() && !first.contains(nullValue)){
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
            firstMap[chave] = listOf(nullValue)
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
                                    it.remove(nullValue)
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
            if(numElementos >= 2){
                for(i in 1 until numElementos){
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