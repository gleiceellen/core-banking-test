package br.com.itau.challenge.hello.application

import br.com.itau.challenge.hello.port.input.InitializeAccountUseCase
import br.com.itau.challenge.hello.port.output.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InitializeAccountService(
    private val accountRepository: AccountRepository,
) : InitializeAccountUseCase {

    private val logger = LoggerFactory.getLogger(InitializeAccountService::class.java)

    override fun initialize(accountId: UUID, owner: UUID, createdAtMicros: Long, status: String) {
        if (status != "ENABLED") {
            logger.info("Evento ignorado accountId={} status={}", accountId, status)
            return
        }
        accountRepository.createIfAbsent(accountId.toString(), owner.toString(), createdAtMicros)
        logger.info("Conta inicializada accountId={} owner={}", accountId, owner)
    }
}
