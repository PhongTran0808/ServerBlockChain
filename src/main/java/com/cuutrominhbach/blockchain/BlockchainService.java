package com.cuutrominhbach.blockchain;

import com.cuutrominhbach.exception.BlockchainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final long configuredChainId;
    private volatile Long resolvedChainId;
    private volatile FastRawTransactionManager fastTxManager;

    public BlockchainService(Web3j web3j,
                             Credentials credentials,
                             @Qualifier("contractAddress") String contractAddress,
                             @Value("${web3j.chain-id:0}") long configuredChainId) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
        this.configuredChainId = configuredChainId;
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

    /**
     * GIAI ĐOẠN 2: ATOMIC ESCROW (Lệnh Sổ Cái từ Native Blockchain)
     * Encodes: deliverBatch(string province, address citizen, address shop, uint256 amount)
     */
    public String deliverBatch(String province, String citizenAddress, String shopAddress, BigInteger amountInWei) {
        try {
            Function function = new Function(
                    "deliverBatch",
                    Arrays.asList(
                            new Utf8String(province),
                            new Address(citizenAddress),
                            new Address(shopAddress),
                            new Uint256(amountInWei)
                    ),
                    Collections.emptyList()
            );
            return sendTransaction(function);
        } catch (IOException e) {
            log.error("deliverBatch - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("deliverBatch - transaction failed", e);
            throw new BlockchainException("Giao dịch blockchain thất bại: " + e.getMessage());
        }
    }

    /**
     * Best-effort sync of a Merkle root to contract.
     * Expects contract to expose: storeMerkleRoot(bytes32 root)
     */
    public String storeMerkleRoot(String merkleRoot) {
        try {
            byte[] rootBytes = Numeric.hexStringToByteArray(merkleRoot);
            if (rootBytes.length != 32) {
                throw new IllegalArgumentException("Merkle root phải có độ dài bytes32");
            }

            Function function = new Function(
                    "storeMerkleRoot",
                    Collections.singletonList(new Bytes32(rootBytes)),
                    Collections.emptyList()
            );
            return sendTransaction(function);
        } catch (IOException e) {
            log.error("storeMerkleRoot - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("storeMerkleRoot - transaction failed", e);
            throw new BlockchainException("Đồng bộ Merkle root thất bại: " + e.getMessage());
        }
    }

    /**
     * Claim distribution by proof.
     * Expects contract to expose: claimDistribution(address to, uint256 amount, bytes32[] proof)
     */
    public String claimDistribution(String toAddress, BigInteger amount, List<String> proof) {
        try {
            List<Bytes32> proofBytes = proof.stream().map(item -> {
                byte[] raw = Numeric.hexStringToByteArray(item);
                if (raw.length != 32) {
                    throw new IllegalArgumentException("Merkle proof item phải có độ dài bytes32");
                }
                return new Bytes32(raw);
            }).toList();

            Function function = new Function(
                    "claimDistribution",
                    Arrays.asList(
                            new Address(toAddress),
                            new Uint256(amount),
                            new DynamicArray<>(Bytes32.class, proofBytes)
                    ),
                    Collections.emptyList()
            );

            return sendTransaction(function);
        } catch (IOException e) {
            log.error("claimDistribution - RPC connection failed", e);
            throw new BlockchainException("Không thể kết nối đến mạng blockchain");
        } catch (Exception e) {
            log.error("claimDistribution - transaction failed", e);
            throw new BlockchainException("Claim on-chain thất bại: " + e.getMessage());
        }
    }

    private String sendTransaction(Function function) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        FastRawTransactionManager txManager = getTransactionManager();

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

    private FastRawTransactionManager getTransactionManager() throws IOException {
        if (fastTxManager != null) {
            return fastTxManager;
        }

        synchronized (this) {
            if (fastTxManager != null) {
                return fastTxManager;
            }
            long chainId = resolveChainId();
            fastTxManager = new FastRawTransactionManager(web3j, credentials, chainId);
            return fastTxManager;
        }
    }

    private long resolveChainId() throws IOException {
        if (resolvedChainId != null && resolvedChainId > 0) {
            return resolvedChainId;
        }

        synchronized (this) {
            if (resolvedChainId != null && resolvedChainId > 0) {
                return resolvedChainId;
            }

            if (configuredChainId > 0) {
                resolvedChainId = configuredChainId;
                return resolvedChainId;
            }

            BigInteger chainIdFromRpc = web3j.ethChainId().send().getChainId();
            if (chainIdFromRpc == null || chainIdFromRpc.signum() <= 0) {
                throw new IOException("Không lấy được chainId hợp lệ từ RPC");
            }

            resolvedChainId = chainIdFromRpc.longValueExact();
            log.info("Resolved chainId from RPC: {}", resolvedChainId);
            return resolvedChainId;
        }
    }
}
