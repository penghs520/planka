package dev.planka.card.service.expression;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.card.repository.CardRepository;
import dev.planka.domain.card.CardId;
import dev.planka.infra.expression.CardDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardDataProviderImpl implements CardDataProvider {

    private final CardRepository cardRepository;

    @Override
    public CardDTO findCardById(CardId cardId, Yield yield) {
        return cardRepository.findById(cardId, yield, "system").orElse(null);
    }
}
