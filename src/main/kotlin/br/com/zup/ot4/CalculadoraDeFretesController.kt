package br.com.zup.ot4

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import javax.inject.Inject

@Controller("/api/fretes")
class CalculadoraDeFretesController(
    @Inject val gRpcClient: FretesServiceGrpc.FretesServiceBlockingStub
) {

    @Get
    fun calcula(@QueryValue cep: String) : HttpResponse<Any> {
        val request = CalculaFreteRequest.newBuilder()
            .setCep(cep)
            .build()

        return try{
            val response = gRpcClient.calculaFrete(request)
            HttpResponse.ok(FreteResponse(response.cep,response.valor))
        } catch (e: StatusRuntimeException){
            e.printStackTrace()
            when (e.status.code) {
                Status.Code.INVALID_ARGUMENT -> throw HttpStatusException(HttpStatus.BAD_REQUEST, e.status.description)
                Status.Code.PERMISSION_DENIED -> {
                    val statusProto = StatusProto.fromThrowable(e)
                        ?: throw HttpStatusException(HttpStatus.FORBIDDEN, e.status.description)
                    val details = statusProto.detailsList[0].unpack(ErrorDetails::class.java)
                    throw HttpStatusException(HttpStatus.FORBIDDEN, "${details.code}: ${details.message}")
                }
                else -> throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
            }
        }
    }
}

