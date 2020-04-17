package com.example.memorycanvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class Card {
    Paint p = new Paint();
    int color;
    int backColor = Color.GRAY;
    boolean isOpen = false; // цвет карты
    float x, y, width, height;

    public Card(float x, float y, float width, float height, int color) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void draw(Canvas c) {
        // нарисовать карту в виде цветного прямоугольника
        if (isOpen) {
            p.setColor(color);
        } else p.setColor(backColor);
        c.drawRect(x, y, x + width, y + height, p);
    }

    public boolean flip(float touch_x, float touch_y) {
        if (touch_x >= x && touch_x <= x + width && touch_y >= y && touch_y <= y + height) {
            isOpen = !isOpen;
            return true;
        } else return false;
    }
}


public class TilesView extends View {
    // пауза для запоминания карт
    final int PAUSE_LENGTH = 1; // в секундах
    boolean isOnPauseNow = false;

    // число открытых карт
    int openedCard = 0;
    ArrayList<Card> cards = new ArrayList<>();
    int width, height; // ширина и высота канвы

    boolean init_field = false;
    int k1 = 4, k2 = 3; // 4x3
    int[][] tiles;  // массив 2*n цветов
    Card firstCard, secondCard;
    boolean stop_draw = false;

    public TilesView(Context context) {
        super(context);
    }

    public TilesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // заполнить массив tiles случайными цветами
        // сгенерировать поле 2*n карт, при этом
        // должно быть ровно n пар карт разных цветов

        int[] rgb = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
        int[] pair = new int[rgb.length];
        Arrays.fill(pair, 2);

        int k;
        tiles = new int[k1][k2];
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                while (tiles[i][j] == 0) {
                    Random r = new Random();
                    k = r.nextInt(rgb.length);
                    if (pair[k] > 0) {
                        tiles[i][j] = rgb[k];
                        pair[k]--;
                    }
                }
            }
        }
    }

    public void init(int w, int h) {
        int a = (int) (Math.min(w, h) * 0.8 / 3); //length
        int b = (int) (Math.max(w, h) * 0.8 / 4); //height
        int offset = (int) (Math.min(w, h) * 0.1 / 4); //offset_between
        int f1 = (int) ((Math.min(w, h) - a * k2 - offset * (k2 - 1)) / 2); //offset_for_center
        int f2 = (int) ((Math.max(w, h) - b * k1 - offset * (k1 - 1)) / 2); //offset_for_center

        for (int i = 0; i < k1; i++) {
            for (int j = 0; j < k2; j++) {
                int p = tiles[i][j];
                cards.add(new Card((a + offset) * j + f1, (b + offset) * i + f2, a, b, p));
            }
        }
    }

    public void checkOpenCardsEqual(Card card1, Card card2) {
        if (card1.color == card2.color) {
            cards.remove(card1);
            cards.remove(card2);
        }
    }

    public void setWinBackground(){
        setBackground(getResources().getDrawable(R.drawable.bg_congratulation));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // отрисовка плиток
        width = canvas.getWidth();
        height = canvas.getHeight();

        if (!init_field) {
            init(width, height);
            init_field = true;
        }

        // проверить, остались ли ещё карты
        // иначе сообщить об окончании игры
        if (cards.size() > 0) {
            for (Card c : cards) {
                c.draw(canvas);
            }
        } else {
            if(!stop_draw){
                stop_draw = true;
                setWinBackground();
                Toast.makeText(getContext(), "Поздравляем!!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // координаты касания
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN && !isOnPauseNow) {
            // палец коснулся экрана
            for (Card c : cards) {
                if (openedCard == 0) {
                    if (c.flip(x, y)) {
                        Log.d("mytag", "card flipped: " + openedCard);
                        openedCard++;
                        invalidate();
                        firstCard = c;
                        return true;
                    }
                }

                if (openedCard == 1) {
                    // перевернуть карту с задержкой
                    if (c.flip(x, y)) {
                        openedCard++;
                        secondCard = c;
                        invalidate();
                        PauseTask task = new PauseTask();
                        task.execute(PAUSE_LENGTH);
                        isOnPauseNow = true;
                        return true;
                    }
                }
            }
        }
        return true;
    }

    public void newGame() {
        // запуск новой игры
        TilesView tl = new TilesView(getContext(),null);
        init_field = false;
        this.tiles = tl.tiles;
        this.cards = tl.cards;
        setBackgroundColor(Color.rgb(224, 255, 255));
        stop_draw = false;
        invalidate();
    }

    class PauseTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... integers) {
            Log.d("mytag", "Pause started");
            try {
                Thread.sleep(integers[0] * 1000); // передаём число секунд ожидания
            } catch (InterruptedException e) {
            }
            Log.d("mytag", "Pause finished");
            return null;
        }

        // после паузы, перевернуть все карты обратно
        @Override
        protected void onPostExecute(Void aVoid) {
            for (Card c : cards) {
                if (c.isOpen) {
                    c.isOpen = false;
                }
            }
            openedCard = 0;
            isOnPauseNow = false;
            // если открылись карты одинакового цвета, удалить их из списка
            checkOpenCardsEqual(firstCard, secondCard);
            invalidate();
        }
    }
}