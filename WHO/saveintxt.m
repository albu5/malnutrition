csvwrite('Day.txt', Day(2:end)');
csvwrite('SD0.txt', SD0(2:end)');
csvwrite('SD1.txt', SD1(2:end)');
csvwrite('SD2.txt', SD2(2:end)');
csvwrite('SD3.txt', SD3(2:end)');
csvwrite('SD4.txt', SD4(2:end)');
csvwrite('SD1neg.txt', SD1neg(2:end)');
csvwrite('SD2neg.txt', SD2neg(2:end)');
csvwrite('SD3neg.txt', SD3neg(2:end)');
csvwrite('SD4neg.txt', SD4neg(2:end)');
%%
csvwrite('lfagirls.csv', [Day(2:end), SD0(2:end), SD1(2:end) SD2(2:end), SD3(2:end), SD4(2:end), SD1neg(2:end), SD2neg(2:end), SD3neg(2:end) SD4neg(2:end)]);
