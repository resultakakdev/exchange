/*
 * This file is part of bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.core.trade.protocol.tasks.seller;

import io.bisq.common.taskrunner.TaskRunner;
import io.bisq.core.btc.AddressEntry;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.trade.Trade;
import io.bisq.core.trade.protocol.tasks.TradeTask;
import io.bisq.network.p2p.SendMailboxMessageListener;
import io.bisq.protobuffer.message.trade.FinalizePayoutTxRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellerAsOffererSendFinalizePayoutTxRequest extends TradeTask {
    private static final Logger log = LoggerFactory.getLogger(SellerAsOffererSendFinalizePayoutTxRequest.class);

    @SuppressWarnings({"WeakerAccess", "unused"})
    public SellerAsOffererSendFinalizePayoutTxRequest(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            //TODO: locktime  
            runInterceptHook();
            if (trade.getTradingPeerNodeAddress() != null) {
                BtcWalletService walletService = processModel.getWalletService();
                String sellerPayoutAddress = walletService.getOrCreateAddressEntry(processModel.getOffer().getId(), AddressEntry.Context.TRADE_PAYOUT).getAddressString();
                FinalizePayoutTxRequest message = new FinalizePayoutTxRequest(
                        processModel.getId(),
                        processModel.getPayoutTxSignature(),
                        sellerPayoutAddress,
                      /*  trade.getLockTimeAsBlockHeight(),*/
                        processModel.getMyNodeAddress()
                );

                processModel.getP2PService().sendEncryptedMailboxMessage(
                        trade.getTradingPeerNodeAddress(),
                        processModel.tradingPeer.getPubKeyRing(),
                        message,
                        new SendMailboxMessageListener() {
                            @Override
                            public void onArrived() {
                                log.trace("Message arrived at peer.");
                                trade.setState(Trade.State.SELLER_AS_OFFERER_SENT_FIAT_PAYMENT_RECEIPT_MSG);
                                complete();
                            }

                            @Override
                            public void onStoredInMailbox() {
                                log.trace("Message stored in mailbox.");
                                trade.setState(Trade.State.SELLER_AS_OFFERER_SENT_FIAT_PAYMENT_RECEIPT_MSG);
                                complete();
                            }

                            @Override
                            public void onFault(String errorMessage) {
                                appendToErrorMessage("FinalizePayoutTxRequest sending failed. errorMessage=" + errorMessage);
                                failed(errorMessage);
                            }
                        }
                );
            } else {
                log.error("trade.getTradingPeerAddress() = " + trade.getTradingPeerNodeAddress());
                failed("A needed dependency is null");
            }
        } catch (Throwable t) {
            failed(t);
        }
    }
}