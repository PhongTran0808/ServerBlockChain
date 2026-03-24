package com.cuutrominhbach.blockchain;

import com.cuutrominhbach.exception.BlockchainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;

    public BlockchainService(Web3j web3j,
                             Credentials credentials,
                             @Qualifier("contractAddress") String contractAddress) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
    }

    /**
     * Mint ERC-1155 token to a recipient address.
     * Encodes: mint(address to, uint256 id, uint256 amount, bytes data)
     */
    public String mintToken(String toAddress, BigInteger tokenId, BigInteger amount) {
        try {
            Function function = new Function(
                    "mint",
                    Arrays.asList(
                            new Address(toAddress),
                            new Uint256(tokenId),
                            new Uint256(amount),
                            new DynamicBytes(new byte[0])
                    ),
                    Collections.emptyList()
            );
            return sendTransaction(function);
        } catch (IOException e) {
            log.error("mintToken - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("mintToken - transaction failed", e);
            throw new BlockchainException("Giao dịch blockchain thất bại: " + e.getMessage());
        }
    }

    /**
     * Lock tokens in escrow for an order.
     * Encodes: lockTokens(address citizen, uint256 orderId, uint256 amount)
     */
    public String lockTokens(String citizenAddress, BigInteger orderId, BigInteger amount) {
        try {
            Function function = new Function(
                    "lockTokens",
                    Arrays.asList(
                            new Address(citizenAddress),
                            new Uint256(orderId),
                            new Uint256(amount)
                    ),
                    Collections.emptyList()
            );
            return sendTransaction(function);
        } catch (IOException e) {
            log.error("lockTokens - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("lockTokens - transaction failed", e);
            throw new BlockchainException("Giao dịch blockchain thất bại: " + e.getMessage());
        }
    }

    /**
     * Release locked tokens for a completed order.
     * Encodes: releaseTokens(uint256 orderId)
     */
    public String releaseTokens(BigInteger orderId) {
        try {
            Function function = new Function(
                    "releaseTokens",
                    Collections.singletonList(new Uint256(orderId)),
                    Collections.emptyList()
            );
            return sendTransaction(function);
        } catch (IOException e) {
            log.error("releaseTokens - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("releaseTokens - transaction failed", e);
            throw new BlockchainException("Giao dịch blockchain thất bại: " + e.getMessage());
        }
    }

    /**
     * Transfer ERC-1155 token between addresses.
     * Encodes: safeTransferFrom(address from, address to, uint256 id, uint256 amount, bytes data)
     */
    public String transferToken(String from, String to, BigInteger tokenId, BigInteger amount) {
        try {
            Function function = new Function(
                    "safeTransferFrom",
                    Arrays.asList(
                            new Address(from),
                            new Address(to),
                            new Uint256(tokenId),
                            new Uint256(amount),
                            new DynamicBytes(new byte[0])
                    ),
                    Collections.emptyList()
            );
            return sendTransaction(function);
        } catch (IOException e) {
            log.error("transferToken - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("transferToken - transaction failed", e);
            throw new BlockchainException("Giao dịch blockchain thất bại: " + e.getMessage());
        }
    }

    private String sendTransaction(Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        RawTransactionManager txManager = new RawTransactionManager(web3j, credentials);

        EthSendTransaction ethSendTransaction = txManager.sendTransaction(
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        if (ethSendTransaction.hasError()) {
            throw new BlockchainException(
                    "Giao dịch blockchain thất bại: " + ethSendTransaction.getError().getMessage()
            );
        }

        return ethSendTransaction.getTransactionHash();
    }
}
