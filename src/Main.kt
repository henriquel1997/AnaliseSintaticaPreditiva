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

fun main(){
    val gramatica = """
        <expressao>::=<termo><expressao_linha>
        <expressao_linha>::=+<termo><expressao_linha>|-<termo><expressao_linha>|null
        <termo>::=<fator><termo_linha>
        <termo_linha>::*<fator><termo_linha>|/<fator><termo_linha>|null
        <fator>::=(<expressao>)|<num>|<ide>
        <num>::=<dig><num_linha>
        <num_linha>::=<dig><num_linha>|null
        <ide>::=<letra><ide_linha>
        <ide_linha>::=<letra><ide_linha>|<dig><ide_linha>|null
        <dig>::=0|1|2|3|4|5|6|7|8|9
        <letra>::=A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z
    """.trimIndent()

    //<letra>::=A|B|C

    processarGramatica(gramatica)
    gerarFirst()
    mostrarFirsts()
}

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
        }else if(c == 'n' && texto.length >= i+4 && texto.substring(i, i+4) == "null"){
            elemento.nome = "null"
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

fun gerarFirst(){
    for(regra in regras){
        gerarFirst(regra)
    }
    removerTerminaisFirst()
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
            firstMap[primeiroElemento.nome] = listOf("null")
            first.add("null")
        }

        Tipo.NAO_TERMINAL -> {
            for(elemento in subRegra.elementos){
                if(elemento.nome != nomeRegra){
                    if(!firstMap.containsKey(elemento.nome)){
                        gerarFirstElemento(elemento)
                    }
                    val firstElemento = firstMap[elemento.nome]
                    if(firstElemento != null){
                        if(first.contains("null") && !firstElemento.contains("null")){
                            first.remove("null")
                        }
                        first = first.union(firstElemento).toMutableList()
                    }
                    if(first.isNotEmpty() && !first.contains("null")){
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
            firstMap[chave] = listOf("null")
        }

        Tipo.NAO_TERMINAL -> {
            regras.firstOrNull { it.nome == chave }?.let {
                gerarFirst(it)
            }
        }
    }
}

fun removerTerminaisFirst(){
    val chaves = mutableListOf<String>()
    for(chave in firstMap.keys){
        if(firstMap[chave]?.size == 1){
            chaves.add(chave)
        }
    }
    for(chave in chaves){
        firstMap.remove(chave)
    }
}

fun mostrarFirsts(){
    for(chave in firstMap.keys){
        var first = ""
        firstMap[chave]?.let {
            for(string in it){
                if(first.isEmpty()){
                    first = string
                }else{
                    first += ", $string"
                }
            }
        }

        println("$chave: $first")
    }
}