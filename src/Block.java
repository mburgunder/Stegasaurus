import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Mitchell on 3/3/2016.
 */
public class Block {

    private static final Block WHITE = new Block(0, 0);
    private static final Block BLACK = new Block(1, 1);
    private static final Block W_CHECK = new Block(1, 0);
    private static final Block B_CHECK = new Block(0, 1);

    private byte[] block;
    private int x;
    private int y;
    private int bit;
    private Channel channel;

    private Block(int pairMatch, int pairMismatch){
        block = new byte[8];
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if(i + j % 2 == 0){
                    block[i] = (byte)((block[i] << 1) | pairMatch);
                }
                else{
					block[i] = (byte)((block[i] << 1) | pairMatch);
                }
            }
        }
        this.x = 0;
        this.y = 0;
        this.bit = 0;
        this.channel = Channel.GREY;
    }

    public Block(BufferedImage img, int x, int y, int bit, Channel channel){
        //error checking
        if(x < 0 || y < 0){
            throw new IllegalArgumentException("x and y must be positive");
        }
        if(bit < 0 || bit >= 8){
            throw new IllegalArgumentException("bit must be between 0 and 7");
        }

        block = new byte[8];
        int selector = (1 << bit);
        //System.out.println("Selector:" + selector);
        for(int i = x; i < x + 8; i++){
			if(i >= img.getWidth()){
				break;
			}

            for(int j = y; j < y + 8; j++){
				if(j >= img.getHeight()){
					break;
				}
                Color c = new Color(img.getRGB(i,j));
                int channelVal = 0;
                switch(channel){
                    case RED:
                        channelVal = c.getRed();
                        break;
                    case GREEN:
                        channelVal = c.getGreen();
                        break;
                    case BLUE:
                        channelVal = c.getBlue();
                        break;
                    default:
                        channelVal = selector;
                        break;
                }
				//TODO verify this is good
				//System.out.printf("(Image constructor)accessing location [%d][%d]%n", (i-x), (8 - (j-y) - 1));
                if((channelVal & selector) == selector){
                    block[i-x] = (byte) (block[i-x] | (1 << (8 - (j - y) - 1)));
                }
                else{
					block[i-x] = (byte) (block[i-x] | (0 << (8 - (j - y))));
                }

            }
        }
        this.x = x;
        this.y = y;
        this.bit = bit;
        this.channel = channel;

    }

    public Block(byte[] message){
		if(message.length > 8){
			throw new IllegalArgumentException("Message must be 8 bytes long");
		}
		if(message.length < 8){
			message = Arrays.copyOf(message, 8);
		}
		block = new byte[8];
		for(int i = 0; i < 8; i++){
			block[i] = message[i];
		}
	}

	public static List<Block> blockify(byte[] bytes){
		List<Block> blocks = new LinkedList<Block>();

		int index = 0;
		while(index + 7 < bytes.length){
			int numBytes = Math.min(bytes.length - index, 7);
			byte[] subBlock = Arrays.copyOfRange(bytes, index, index + numBytes);
			byte[] checkByte = new byte[]{(byte) (0xff & subBlock.length)};
			//System.out.println("checkByte = " + checkByte);
			byte[] byteBlock = new byte[8];
			System.arraycopy(checkByte, 0, byteBlock, 0, checkByte.length);
			System.arraycopy(subBlock, 0, byteBlock, checkByte.length, subBlock.length);
			Block block = new Block(byteBlock);
			blocks.add(block);
			index += numBytes;
		}
		///include a block indicating the total length of the other blocks
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.put((byte)0xf4);
		buffer.putInt(blocks.size());
		blocks.add(0, new Block(buffer.array()));

		return blocks;
	}

	public static byte[] unblockify(List<Block> blocks){
		byte[] bytes = {};



		return bytes;
	}

    public byte[] getByteBlock(){
        return block;
    }

	//TODO verify this is good
    public int[][] getIntBlock(){
		int[][] intBlock = new int[8][8];
		int ones = 0xffffffff;
		int zeros = 0x00000000;
		int bitmask = 1 << bit;
		switch(channel){
			case RED:
				bitmask = bitmask << 16;
				break;
			case GREEN:
				bitmask = bitmask << 8;
				break;
			case BLUE:
				bitmask = bitmask << 0;
				break;
			default:
				bitmask = bitmask << 0;
				break;
		}

		//TODO verify this is good
		for(int i = 0; i < 8; i++){
			for(int j = 0; j < 8; j++){
				if((block[i] >> j) % 2 == 0){
					intBlock[i][j] = bitmask & zeros;
				}
				else{
					intBlock[i][j] = bitmask & ones;
				}
			}
		}

		return intBlock;
	}

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public int getBit(){
        return bit;
    }

    public Channel getChannel(){
        return channel;
    }

    public void xor(Block other){
        byte[] otherBlock = other.getByteBlock();
        for(int i = 0; i < 8; i++){
			block[i] = (byte) (block[i] ^ otherBlock[i]);
        }
    }

	public void conjugate(){
		this.xor(W_CHECK);
	}

    public float getComplexity(){
        float flips = 0;
        float total = 0;

		for(int i = 0; i < 8; i++){
			int lastPairH = block[i] >> 0;
			int lastPairV = block[0] >> i;
			for(int j = 0; j < 8; j++){
				int thisPairH = block[i] >> j;
				int thisPairV = block[j] >> i;
				if((lastPairH % 2) != (thisPairH % 2)){
					flips++;
				}
				total++;
				lastPairH = thisPairH;
				if((lastPairV % 2) != (thisPairV % 2)){
					flips++;
				}
				total++;
				lastPairV = thisPairV;
			}
		}

        return flips / total;
    }

	public void copyMetadata(Block other){
		this.x = other.getX();
		this.y = other.getY();
		this.bit = other.getBit();
		this.channel = other.getChannel();
	}

    /*
    ** Mixed role anti-cohesion
    public void assimilate(BufferedImage img){
        //insert block into the right location, bit level and channel of the image
    }
    */

}
